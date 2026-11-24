/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.audit;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.vigilex.common.ApiException;
import tn.vigilex.common.PageResponse;
import tn.vigilex.domain.AuditLog;
import tn.vigilex.tenant.TenantContext;

@RestController
@RequestMapping("/api/v1/audit-log")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit) { this.audit = audit; }
    @GetMapping
    public PageResponse<AuditLog> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 200) throw ApiException.badRequest("Invalid pagination bounds");
        return PageResponse.from(audit.list(TenantContext.requireTenantId(), PageRequest.of(page, size)));
    }
}
