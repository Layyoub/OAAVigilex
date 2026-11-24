/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.analysis;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.audit.AuditService;
import tn.vigilex.common.ApiException;
import tn.vigilex.domain.*;
import tn.vigilex.notification.NotificationService;
import tn.vigilex.repository.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class TenantTextService {
    private static final Set<String> CONFORMITY = Set.of("NOT_ANALYZED", "NON_COMPLIANT",
            "PARTIALLY_COMPLIANT", "CANDIDATE_COMPLIANT", "COMPLIANT");
    private static final Set<String> APPLICABILITY = Set.of("APPLICABLE", "TO_REVIEW", "INFORMATION");
    private final TenantTextRepository tenantTexts;
    private final LawTextRepository lawTexts;
    private final ActionRepository actions;
    private final RequirementRepository requirements;
    private final ConformityHistoryRepository history;
    private final AppUserRepository users;
    private final AuditService audit;
    private final NotificationService notifications;
    private final DepartmentRepository departments;
    private final StakeholderRepository stakeholders;
    private final TextDepartmentRepository textDepartments;
    private final TextStakeholderRepository textStakeholders;

    public TenantTextService(TenantTextRepository tenantTexts, LawTextRepository lawTexts,
                             ActionRepository actions, RequirementRepository requirements,
                             ConformityHistoryRepository history, AppUserRepository users,
                             AuditService audit, NotificationService notifications,
                             DepartmentRepository departments, StakeholderRepository stakeholders,
                             TextDepartmentRepository textDepartments, TextStakeholderRepository textStakeholders) {
        this.tenantTexts = tenantTexts;
        this.lawTexts = lawTexts;
        this.actions = actions;
        this.requirements = requirements;
        this.history = history;
        this.users = users;
        this.audit = audit;
        this.notifications = notifications;
        this.departments = departments;
        this.stakeholders = stakeholders;
        this.textDepartments = textDepartments;
        this.textStakeholders = textStakeholders;
    }

    @Transactional
    public List<TenantText> addAll(UUID tenantId, UUID actorId, List<UUID> lawTextIds) {
        if (lawTextIds == null || lawTextIds.isEmpty() || lawTextIds.size() > 500)
            throw ApiException.badRequest("Provide between 1 and 500 library text ids");
        LinkedHashMap<UUID, TenantText> created = new LinkedHashMap<>();
        for (UUID lawTextId : new LinkedHashSet<>(lawTextIds)) {
            if (!lawTexts.existsById(lawTextId)) throw ApiException.notFound("Library text not found: " + lawTextId);
            if (tenantTexts.findByTenantIdAndLawTextId(tenantId, lawTextId).isEmpty()) {
                TenantText item = new TenantText();
                item.setTenantId(tenantId);
                item.setLawTextId(lawTextId);
                item.setUpdatedAt(Instant.now());
                created.put(lawTextId, tenantTexts.save(item));
            }
        }
        created.values().forEach(item -> audit.record(tenantId, actorId, "tenant_text", item.getId(), "CREATE",
                Map.of("lawTextId", item.getLawTextId())));
        return List.copyOf(created.values());
    }

    @Transactional(readOnly = true)
    public Page<TenantText> list(UUID tenantId, Pageable pageable) {
        return tenantTexts.findAllByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public TenantText get(UUID tenantId, UUID id) {
        return tenantTexts.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Text is not in this organization's register"));
    }

    @Transactional
    public TenantText update(UUID tenantId, UUID actorId, UUID id, UpdateRequest request) {
        TenantText item = get(tenantId, id);
        if (request.applicability() != null) {
            String value = request.applicability().toUpperCase(Locale.ROOT);
            if (!APPLICABILITY.contains(value)) throw ApiException.badRequest("Unsupported applicability");
            item.setApplicability(value);
        }
        if (request.applicabilityReason() != null) item.setApplicabilityReason(request.applicabilityReason());
        if (request.ownerId() != null) {
            users.findByIdAndTenantId(request.ownerId(), tenantId)
                    .orElseThrow(() -> ApiException.badRequest("Owner must belong to this organization"));
            item.setOwnerId(request.ownerId());
        }
        if (request.nextReviewOn() != null) item.setNextReviewOn(request.nextReviewOn());
        if (request.departmentIds() != null) {
            List<UUID> ids = request.departmentIds().stream().distinct().toList();
            if (ids.stream().anyMatch(departmentId -> departments.findByIdAndTenantId(departmentId, tenantId).isEmpty()))
                throw ApiException.badRequest("Every department must belong to this organization");
            textDepartments.deleteAllByTenantTextId(id);
            ids.forEach(departmentId -> {
                TextDepartment link = new TextDepartment();
                link.setTenantTextId(id);
                link.setDepartmentId(departmentId);
                textDepartments.save(link);
            });
        }
        if (request.stakeholderIds() != null) {
            List<UUID> ids = request.stakeholderIds().stream().distinct().toList();
            if (ids.stream().anyMatch(stakeholderId -> stakeholders.findByIdAndTenantId(stakeholderId, tenantId).isEmpty()))
                throw ApiException.badRequest("Every stakeholder must belong to this organization");
            textStakeholders.deleteAllByTenantTextId(id);
            ids.forEach(stakeholderId -> {
                TextStakeholder link = new TextStakeholder();
                link.setTenantTextId(id);
                link.setStakeholderId(stakeholderId);
                textStakeholders.save(link);
            });
        }
        item.setUpdatedAt(Instant.now());
        TenantText saved = tenantTexts.save(item);
        audit.record(tenantId, actorId, "tenant_text", id, "UPDATE", Map.of(
                "applicability", saved.getApplicability(), "ownerId", Objects.toString(saved.getOwnerId(), "")));
        return saved;
    }

    @Transactional
    public TenantText changeConformity(UUID tenantId, UUID actorId, String role, UUID id,
                                       String requestedStatus, String note) {
        TenantText item = get(tenantId, id);
        String target = requestedStatus.toUpperCase(Locale.ROOT);
        if (!CONFORMITY.contains(target)) throw ApiException.badRequest("Unsupported conformity status");
        List<ActionItem> relatedActions = actions.findAllByTenantIdAndTenantTextId(tenantId, id);
        if ("CANDIDATE_COMPLIANT".equals(target)
                && relatedActions.stream().anyMatch(action -> !"DONE".equals(action.getStatus())))
            throw ApiException.badRequest("Candidate compliance requires all linked actions to be completed");
        if ("COMPLIANT".equals(target)) {
            if (!Set.of("TENANT_ADMIN", "RESP_CONFORMITE").contains(role))
                throw ApiException.forbidden();
            if (relatedActions.stream().anyMatch(action -> !"DONE".equals(action.getStatus())
                    || !"EFFECTIVE".equals(action.getEffectiveness())))
                throw ApiException.badRequest("Compliance validation requires completed, effective actions");
        }
        String previous = item.getConformity();
        item.setConformity(target);
        item.setVerifiedOn(LocalDate.now());
        item.setUpdatedAt(Instant.now());
        tenantTexts.save(item);
        ConformityHistory change = new ConformityHistory();
        change.setTenantId(tenantId);
        change.setTenantTextId(id);
        change.setFromStatus(previous);
        change.setToStatus(target);
        change.setChangedBy(actorId);
        change.setNote(note);
        history.save(change);
        audit.record(tenantId, actorId, "tenant_text", id, "CONFORMITY_CHANGE",
                Map.of("from", previous, "to", target));
        return item;
    }

    @Transactional(readOnly = true)
    public List<ConformityHistory> history(UUID tenantId, UUID itemId) {
        get(tenantId, itemId);
        return history.findAllByTenantIdAndTenantTextIdOrderByChangedAtDesc(tenantId, itemId);
    }

    @Transactional
    public Requirement addRequirement(UUID tenantId, UUID actorId, UUID tenantTextId,
                                      String articleRef, String description) {
        TenantText parent = get(tenantId, tenantTextId);
        Requirement requirement = new Requirement();
        requirement.setTenantTextId(parent.getId());
        requirement.setArticleRef(articleRef);
        requirement.setDescription(description);
        Requirement saved = requirements.save(requirement);
        audit.record(tenantId, actorId, "requirement", saved.getId(), "CREATE", Map.of("tenantTextId", tenantTextId));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Requirement> requirements(UUID tenantId, UUID tenantTextId) {
        get(tenantId, tenantTextId);
        return requirements.findAllByTenantTextId(tenantTextId);
    }

    @Transactional
    public void actionCreated(UUID tenantId, UUID actorId, TenantText text, UUID actionId) {
        if (Set.of("COMPLIANT", "CANDIDATE_COMPLIANT").contains(text.getConformity())) {
            String previous = text.getConformity();
            text.setConformity("PARTIALLY_COMPLIANT");
            text.setUpdatedAt(Instant.now());
            tenantTexts.save(text);
            ConformityHistory change = new ConformityHistory();
            change.setTenantId(tenantId);
            change.setTenantTextId(text.getId());
            change.setFromStatus(previous);
            change.setToStatus("PARTIALLY_COMPLIANT");
            change.setChangedBy(actorId);
            change.setNote("An open corrective action was added");
            history.save(change);
            notifications.notifyTenantAdmins(tenantId, "CONFORMITY_REOPENED",
                    Map.of("tenantTextId", text.getId(), "actionId", actionId));
            audit.record(tenantId, actorId, "tenant_text", text.getId(), "CONFORMITY_CHANGE",
                    Map.of("from", previous, "to", "PARTIALLY_COMPLIANT"));
        }
    }

    @Transactional
    public void reconcileAfterActionChange(UUID tenantId, UUID actorId, UUID tenantTextId) {
        TenantText text = get(tenantId, tenantTextId);
        List<ActionItem> relatedActions = actions.findAllByTenantIdAndTenantTextId(tenantId, tenantTextId);
        boolean invalidated = "COMPLIANT".equals(text.getConformity())
                ? relatedActions.stream().anyMatch(action -> !"DONE".equals(action.getStatus())
                    || !"EFFECTIVE".equals(action.getEffectiveness()))
                : "CANDIDATE_COMPLIANT".equals(text.getConformity())
                    && relatedActions.stream().anyMatch(action -> !"DONE".equals(action.getStatus()));
        if (!invalidated) return;
        String previous = text.getConformity();
        text.setConformity("PARTIALLY_COMPLIANT");
        text.setUpdatedAt(Instant.now());
        tenantTexts.save(text);
        ConformityHistory change = new ConformityHistory();
        change.setTenantId(tenantId);
        change.setTenantTextId(tenantTextId);
        change.setFromStatus(previous);
        change.setToStatus("PARTIALLY_COMPLIANT");
        change.setChangedBy(actorId);
        change.setNote("A linked action no longer satisfies the conformity criteria");
        history.save(change);
        notifications.notifyTenantAdmins(tenantId, "CONFORMITY_REOPENED", Map.of("tenantTextId", tenantTextId));
        audit.record(tenantId, actorId, "tenant_text", tenantTextId, "CONFORMITY_CHANGE",
                Map.of("from", previous, "to", "PARTIALLY_COMPLIANT"));
    }

    public record UpdateRequest(String applicability, String applicabilityReason, UUID ownerId,
                                LocalDate nextReviewOn, List<UUID> departmentIds, List<UUID> stakeholderIds) { }
}
