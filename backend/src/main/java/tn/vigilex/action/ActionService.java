/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.action;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.analysis.TenantTextService;
import tn.vigilex.audit.AuditService;
import tn.vigilex.common.ApiException;
import tn.vigilex.domain.ActionItem;
import tn.vigilex.domain.AppUser;
import tn.vigilex.domain.TenantText;
import tn.vigilex.repository.ActionRepository;
import tn.vigilex.repository.AppUserRepository;
import tn.vigilex.repository.TenantTextRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ActionService {
    private static final Set<String> STATUSES = Set.of("TO_DO", "IN_PROGRESS", "DONE");
    private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private static final Set<String> EFFECTIVENESS = Set.of("UNDEFINED", "EFFECTIVE", "INEFFECTIVE");
    private final ActionRepository actions;
    private final TenantTextRepository tenantTexts;
    private final AppUserRepository users;
    private final TenantTextService tenantTextService;
    private final AuditService audit;

    public ActionService(ActionRepository actions, TenantTextRepository tenantTexts, AppUserRepository users,
                         TenantTextService tenantTextService, AuditService audit) {
        this.actions = actions;
        this.tenantTexts = tenantTexts;
        this.users = users;
        this.tenantTextService = tenantTextService;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<ActionItem> list(UUID tenantId, UUID userId, String role, String status, String priority,
                                 UUID responsibleId, LocalDate from, LocalDate to, Pageable pageable) {
        Specification<ActionItem> filter = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if ("CONTRIBUTEUR".equals(role)) predicates.add(cb.equal(root.get("responsibleId"), userId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            if (priority != null) predicates.add(cb.equal(root.get("priority"), priority.toUpperCase()));
            if (responsibleId != null) predicates.add(cb.equal(root.get("responsibleId"), responsibleId));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("dueOn"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("dueOn"), to));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return actions.findAll(filter, pageable);
    }

    @Transactional
    public ActionItem create(UUID tenantId, UUID actorId, CreateRequest request) {
        TenantText text = tenantTexts.findByIdAndTenantId(request.tenantTextId(), tenantId)
                .orElseThrow(() -> ApiException.notFound("Text is not in this organization's register"));
        validatePriority(request.priority());
        validateResponsible(tenantId, request.responsibleId());
        ActionItem action = new ActionItem();
        action.setTenantId(tenantId);
        action.setTenantTextId(text.getId());
        action.setTitle(request.title().trim());
        action.setDescription(request.description());
        action.setResponsibleId(request.responsibleId());
        action.setResponsiblePhone(request.responsiblePhone());
        action.setPriority(request.priority() == null ? "MEDIUM" : request.priority().toUpperCase());
        action.setDueOn(request.dueOn());
        ActionItem saved = actions.save(action);
        tenantTextService.actionCreated(tenantId, actorId, text, saved.getId());
        audit.record(tenantId, actorId, "action", saved.getId(), "CREATE",
                Map.of("tenantTextId", text.getId(), "title", saved.getTitle()));
        return saved;
    }

    @Transactional
    public ActionItem update(UUID tenantId, UUID actorId, String role, UUID id, UpdateRequest request) {
        ActionItem action = actions.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Action not found"));
        if ("CONTRIBUTEUR".equals(role) && !actorId.equals(action.getResponsibleId()))
            throw ApiException.forbidden();
        if ("CONTRIBUTEUR".equals(role) && (request.title() != null || request.description() != null
                || request.responsibleId() != null || request.responsiblePhone() != null
                || request.priority() != null || request.dueOn() != null))
            throw ApiException.forbidden();
        if (request.title() != null && !request.title().isBlank()) action.setTitle(request.title().trim());
        if (request.description() != null) action.setDescription(request.description());
        if (request.responsibleId() != null) {
            validateResponsible(tenantId, request.responsibleId());
            action.setResponsibleId(request.responsibleId());
        }
        if (request.responsiblePhone() != null) action.setResponsiblePhone(request.responsiblePhone());
        if (request.priority() != null) {
            validatePriority(request.priority());
            action.setPriority(request.priority().toUpperCase());
        }
        if (request.dueOn() != null) action.setDueOn(request.dueOn());
        if (request.status() != null) {
            String status = request.status().toUpperCase();
            if (!STATUSES.contains(status)) throw ApiException.badRequest("Unsupported action status");
            action.setStatus(status);
            action.setDoneOn("DONE".equals(status) ? LocalDate.now() : null);
            if (!"DONE".equals(status)) {
                action.setEffectiveness("UNDEFINED");
                action.setEffectivenessComment(null);
            }
        }
        if (request.effectiveness() != null) {
            String effectiveness = request.effectiveness().toUpperCase();
            if (!EFFECTIVENESS.contains(effectiveness)) throw ApiException.badRequest("Unsupported effectiveness");
            if (!"DONE".equals(action.getStatus()) && !"UNDEFINED".equals(effectiveness))
                throw ApiException.badRequest("Effectiveness can be assessed only after completion");
            action.setEffectiveness(effectiveness);
            action.setEffectivenessComment(request.effectivenessComment());
        }
        ActionItem saved = actions.save(action);
        tenantTextService.reconcileAfterActionChange(tenantId, actorId, action.getTenantTextId());
        audit.record(tenantId, actorId, "action", id, "UPDATE",
                Map.of("status", saved.getStatus(), "effectiveness", saved.getEffectiveness()));
        return saved;
    }

    @Transactional
    public void delete(UUID tenantId, UUID actorId, UUID id) {
        ActionItem action = actions.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Action not found"));
        actions.delete(action);
        audit.record(tenantId, actorId, "action", id, "DELETE", Map.of());
    }

    @Transactional(readOnly = true)
    public ActionItem get(UUID tenantId, UUID id) {
        return actions.findByIdAndTenantId(id, tenantId).orElseThrow(() -> ApiException.notFound("Action not found"));
    }

    @Transactional(readOnly = true)
    public ActionSummary summary(UUID tenantId) {
        var all = actions.findAllByTenantId(tenantId);
        LocalDate today = LocalDate.now();
        return new ActionSummary(
                all.stream().filter(a -> "IN_PROGRESS".equals(a.getStatus())).count(),
                all.stream().filter(a -> "DONE".equals(a.getStatus())).count(),
                all.stream().filter(a -> !"DONE".equals(a.getStatus()) && a.getDueOn() != null && a.getDueOn().isBefore(today)).count(),
                all.stream().filter(a -> "DONE".equals(a.getStatus()) && "EFFECTIVE".equals(a.getEffectiveness())).count(),
                all.stream().filter(a -> "DONE".equals(a.getStatus()) && "INEFFECTIVE".equals(a.getEffectiveness())).count(),
                all.stream().filter(a -> "DONE".equals(a.getStatus()) && "UNDEFINED".equals(a.getEffectiveness())).count());
    }

    private void validatePriority(String value) {
        if (value != null && !PRIORITIES.contains(value.toUpperCase()))
            throw ApiException.badRequest("Unsupported action priority");
    }

    private void validateResponsible(UUID tenantId, UUID responsibleId) {
        if (responsibleId != null && users.findByIdAndTenantId(responsibleId, tenantId).isEmpty())
            throw ApiException.badRequest("Responsible user must belong to this organization");
    }

    public record CreateRequest(UUID tenantTextId, String title, String description, UUID responsibleId,
                                String responsiblePhone, String priority, LocalDate dueOn) { }
    public record UpdateRequest(String title, String description, UUID responsibleId, String responsiblePhone,
                                String priority, LocalDate dueOn, String status, String effectiveness,
                                String effectivenessComment) { }
    public record ActionSummary(long inProgress, long completed, long overdue, long effective,
                                long ineffective, long effectivenessUndefined) { }
}
