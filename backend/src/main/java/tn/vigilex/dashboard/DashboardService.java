/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.dashboard;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.action.ActionService;
import tn.vigilex.domain.*;
import tn.vigilex.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

@Service
public class DashboardService {
    private static final Map<String, Integer> WEIGHTS = Map.of(
            "COMPLIANT", 100, "CANDIDATE_COMPLIANT", 75, "PARTIALLY_COMPLIANT", 50, "NON_COMPLIANT", 0);
    private final TenantTextRepository tenantTexts;
    private final TenantRepository tenants;
    private final LawTextRepository lawTexts;
    private final ActionRepository actions;
    private final DepartmentRepository departments;
    private final TextDepartmentRepository textDepartments;
    private final ComplianceSnapshotRepository snapshots;
    private final ActionService actionService;

    public DashboardService(TenantTextRepository tenantTexts, TenantRepository tenants, LawTextRepository lawTexts,
                            ActionRepository actions, DepartmentRepository departments,
                            TextDepartmentRepository textDepartments, ComplianceSnapshotRepository snapshots,
                            ActionService actionService) {
        this.tenantTexts = tenantTexts;
        this.tenants = tenants;
        this.lawTexts = lawTexts;
        this.actions = actions;
        this.departments = departments;
        this.textDepartments = textDepartments;
        this.snapshots = snapshots;
        this.actionService = actionService;
    }

    @Transactional(readOnly = true)
    public Kpis kpis(UUID tenantId) {
        Map<String, Long> counts = conformityCounts(tenantId);
        ActionService.ActionSummary actionCounts = actionService.summary(tenantId);
        double score = score(counts);
        return new Kpis(tenantTexts.countByTenantId(tenantId), counts.getOrDefault("NOT_ANALYZED", 0L),
                counts.getOrDefault("NON_COMPLIANT", 0L), counts.getOrDefault("PARTIALLY_COMPLIANT", 0L),
                counts.getOrDefault("CANDIDATE_COMPLIANT", 0L), counts.getOrDefault("COMPLIANT", 0L),
                counts.values().stream().mapToLong(Long::longValue).sum(), score,
                actions.findAllByTenantIdAndDueOnBeforeAndStatusNot(tenantId, LocalDate.now(), "DONE").size(),
                lawTexts.count(), actionCounts, counts,
                actions.findAllByTenantId(tenantId).stream().filter(a -> a.getDueOn() != null
                        && !a.getDueOn().isBefore(LocalDate.now()) && !"DONE".equals(a.getStatus()))
                        .sorted(Comparator.comparing(ActionItem::getDueOn)).limit(10).toList(),
                lawTexts.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "addedAt"))).getContent());
    }

    @Transactional(readOnly = true)
    public List<ComplianceSnapshot> trend(UUID tenantId) {
        return snapshots.findTop12ByTenantIdOrderByTakenOnDesc(tenantId).stream()
                .sorted(Comparator.comparing(ComplianceSnapshot::getTakenOn)).toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentScore> byDepartment(UUID tenantId) {
        List<Department> tenantDepartments = departments.findAllByTenantId(tenantId);
        if (tenantDepartments.isEmpty()) return List.of();
        Map<UUID, Department> departmentById = new HashMap<>();
        tenantDepartments.forEach(department -> departmentById.put(department.getId(), department));
        Map<UUID, List<TenantText>> assigned = new HashMap<>();
        List<UUID> departmentIds = tenantDepartments.stream().map(Department::getId).toList();
        for (TextDepartment link : textDepartments.findAllByDepartmentIdIn(departmentIds)) {
            TenantText text = tenantTexts.findByIdAndTenantId(link.getTenantTextId(), tenantId).orElse(null);
            if (text != null) assigned.computeIfAbsent(link.getDepartmentId(), ignored -> new ArrayList<>()).add(text);
        }
        return tenantDepartments.stream().map(department -> {
            List<TenantText> texts = assigned.getOrDefault(department.getId(), List.of());
            Map<String, Long> counts = new HashMap<>();
            texts.forEach(text -> counts.merge(text.getConformity(), 1L, Long::sum));
            return new DepartmentScore(department.getId(), department.getName(), texts.size(), score(counts), counts);
        }).toList();
    }

    @Transactional
    @Scheduled(cron = "0 10 0 * * *", zone = "Africa/Tunis")
    public void takeNightlySnapshots() {
        LocalDate today = LocalDate.now();
        tenants.findAll().forEach(tenant -> {
            Map<String, Long> counts = conformityCounts(tenant.getId());
            ComplianceSnapshot snapshot = snapshots.findByTenantIdAndTakenOn(tenant.getId(), today)
                    .orElseGet(ComplianceSnapshot::new);
            snapshot.setTenantId(tenant.getId());
            snapshot.setTakenOn(today);
            snapshot.setScore(BigDecimal.valueOf(score(counts)).setScale(2, RoundingMode.HALF_UP));
            snapshot.setCounts(new HashMap<>(counts));
            snapshots.save(snapshot);
        });
    }

    private Map<String, Long> conformityCounts(UUID tenantId) {
        Map<String, Long> counts = new HashMap<>();
        tenantTexts.countConformityByTenant(tenantId)
                .forEach(row -> counts.put((String) row[0], (Long) row[1]));
        return counts;
    }

    private double score(Map<String, Long> counts) {
        long analyzed = 0;
        long weighted = 0;
        for (Map.Entry<String, Integer> weight : WEIGHTS.entrySet()) {
            long count = counts.getOrDefault(weight.getKey(), 0L);
            analyzed += count;
            weighted += count * weight.getValue();
        }
        return analyzed == 0 ? 0.0 : Math.round((weighted * 100.0 / analyzed)) / 100.0;
    }

    public record Kpis(long textsInRegister, long notAnalyzed, long nonCompliant, long partiallyCompliant,
                       long candidateCompliant, long compliant, long totalStatuses, double score,
                       long overdueActions, long generalLibraryTexts, ActionService.ActionSummary actions,
                       Map<String, Long> conformityCounts, List<ActionItem> upcomingActions, List<LawText> recentTexts) { }
    public record DepartmentScore(UUID departmentId, String name, int texts, double score, Map<String, Long> counts) { }
}
