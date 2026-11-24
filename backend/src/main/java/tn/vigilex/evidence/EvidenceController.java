/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.evidence;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.vigilex.domain.Evidence;
import tn.vigilex.security.UserPrincipal;
import tn.vigilex.tenant.TenantContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {
    private final EvidenceService service;
    public EvidenceController(EvidenceService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE','CONTRIBUTEUR')")
    public Evidence upload(@RequestPart("file") MultipartFile file,
                           @RequestParam(required = false) UUID tenantTextId,
                           @RequestParam(required = false) UUID actionId,
                           @AuthenticationPrincipal UserPrincipal user) throws IOException {
        return service.upload(TenantContext.requireTenantId(), user.id(), tenantTextId, actionId,
                file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream());
    }

    @GetMapping
    public List<Evidence> list(@RequestParam(required = false) UUID tenantTextId,
                               @RequestParam(required = false) UUID actionId) {
        return service.list(TenantContext.requireTenantId(), tenantTextId, actionId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        EvidenceService.StoredEvidence file = service.get(TenantContext.requireTenantId(), id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.metadata().getMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.metadata().getFilename(), StandardCharsets.UTF_8).build().toString())
                .contentLength(file.metadata().getSize()).body(file.resource());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE','CONTRIBUTEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        service.delete(TenantContext.requireTenantId(), user.id(), user.role(), id);
        return ResponseEntity.noContent().build();
    }
}
