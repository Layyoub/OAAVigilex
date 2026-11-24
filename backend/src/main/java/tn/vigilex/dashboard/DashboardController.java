/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.dashboard;

import org.springframework.web.bind.annotation.*;
import tn.vigilex.domain.ComplianceSnapshot;
import tn.vigilex.tenant.TenantContext;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }
    @GetMapping("/kpis")
    public DashboardService.Kpis kpis() { return dashboard.kpis(TenantContext.requireTenantId()); }
    @GetMapping("/score-trend")
    public List<ComplianceSnapshot> scoreTrend() { return dashboard.trend(TenantContext.requireTenantId()); }
    @GetMapping("/by-department")
    public List<DashboardService.DepartmentScore> byDepartment() {
        return dashboard.byDepartment(TenantContext.requireTenantId());
    }
}
