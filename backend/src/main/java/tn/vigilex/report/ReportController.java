/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.vigilex.tenant.TenantContext;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reports;
    public ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping(value = "/register.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> excel(@RequestParam(required = false) String standard,
                                        @RequestParam(required = false) UUID departmentId) {
        byte[] report = reports.excel(reports.register(TenantContext.requireTenantId(), standard, departmentId));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vigilex-register.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(report);
    }

    @GetMapping(value = "/register.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) String standard,
                                      @RequestParam(required = false) UUID departmentId) {
        byte[] report = reports.pdf(reports.register(TenantContext.requireTenantId(), standard, departmentId),
                "Registre de conformité");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vigilex-register.pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(report);
    }

    @GetMapping(value = "/audit.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> auditReport(@RequestParam(required = false) String standard,
                                              @RequestParam(required = false) UUID departmentId) {
        byte[] report = reports.pdf(reports.register(TenantContext.requireTenantId(), standard, departmentId),
                "Rapport d'audit réglementaire");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vigilex-audit.pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(report);
    }

    @GetMapping(value = "/register.csv", produces = "text/csv")
    public ResponseEntity<byte[]> csv(@RequestParam(required = false) String standard,
                                      @RequestParam(required = false) UUID departmentId) {
        byte[] report = reports.csv(reports.register(TenantContext.requireTenantId(), standard, departmentId));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vigilex-register.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8")).body(report);
    }
}
