/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.evidence;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.audit.AuditService;
import tn.vigilex.common.ApiException;
import tn.vigilex.domain.ActionItem;
import tn.vigilex.domain.Evidence;
import tn.vigilex.repository.ActionRepository;
import tn.vigilex.repository.EvidenceRepository;
import tn.vigilex.repository.TenantTextRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class EvidenceService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf", "image/jpeg", "image/png",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
    private final EvidenceRepository evidence;
    private final TenantTextRepository tenantTexts;
    private final ActionRepository actions;
    private final StorageService storage;
    private final AuditService audit;

    public EvidenceService(EvidenceRepository evidence, TenantTextRepository tenantTexts, ActionRepository actions,
                          StorageService storage, AuditService audit) {
        this.evidence = evidence;
        this.tenantTexts = tenantTexts;
        this.actions = actions;
        this.storage = storage;
        this.audit = audit;
    }

    @Transactional
    public Evidence upload(UUID tenantId, UUID actorId, UUID tenantTextId, UUID actionId, String originalName,
                           String mimeType, long size, InputStream content) {
        if ((tenantTextId == null) == (actionId == null))
            throw ApiException.badRequest("Attach evidence to exactly one text or action");
        if (size < 1 || size > MAX_SIZE) throw ApiException.badRequest("Evidence file must be at most 10 MiB");
        String mime = mimeType == null ? "" : mimeType.split(";", 2)[0].trim().toLowerCase();
        if (!ALLOWED_MIME.contains(mime)) throw ApiException.badRequest("Unsupported evidence MIME type");
        if (tenantTextId != null && tenantTexts.findByIdAndTenantId(tenantTextId, tenantId).isEmpty())
            throw ApiException.notFound("Text is not in this organization's register");
        if (actionId != null && actions.findByIdAndTenantId(actionId, tenantId).isEmpty())
            throw ApiException.notFound("Action not found");
        String filename = sanitize(originalName);
        String storageKey;
        try {
            storageKey = storage.store(tenantId, content);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Evidence storage failed");
        }
        Evidence item = new Evidence();
        item.setTenantId(tenantId);
        item.setTenantTextId(tenantTextId);
        item.setActionId(actionId);
        item.setFilename(filename);
        item.setMime(mime);
        item.setSize(size);
        item.setUploadedBy(actorId);
        item.setUploadedAt(Instant.now());
        item.setStorageKey(storageKey);
        try {
            Evidence saved = evidence.save(item);
            audit.record(tenantId, actorId, "evidence", saved.getId(), "CREATE", Map.of("filename", filename));
            return saved;
        } catch (RuntimeException ex) {
            try { storage.delete(storageKey); } catch (IOException ignored) { }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<Evidence> list(UUID tenantId, UUID tenantTextId, UUID actionId) {
        if ((tenantTextId == null) == (actionId == null))
            throw ApiException.badRequest("Specify exactly one text or action id");
        if (tenantTextId != null) {
            if (tenantTexts.findByIdAndTenantId(tenantTextId, tenantId).isEmpty())
                throw ApiException.notFound("Text is not in this organization's register");
            return evidence.findAllByTenantIdAndTenantTextId(tenantId, tenantTextId);
        }
        if (actions.findByIdAndTenantId(actionId, tenantId).isEmpty()) throw ApiException.notFound("Action not found");
        return evidence.findAllByTenantIdAndActionId(tenantId, actionId);
    }

    @Transactional(readOnly = true)
    public StoredEvidence get(UUID tenantId, UUID id) {
        Evidence item = evidence.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Evidence not found"));
        try {
            return new StoredEvidence(item, storage.load(item.getStorageKey()));
        } catch (IOException e) {
            throw ApiException.notFound("Evidence file not found");
        }
    }

    @Transactional
    public void delete(UUID tenantId, UUID actorId, String role, UUID id) {
        Evidence item = evidence.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Evidence not found"));
        if ("CONTRIBUTEUR".equals(role) && !item.getUploadedBy().equals(actorId)) throw ApiException.forbidden();
        try {
            storage.delete(item.getStorageKey());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Evidence deletion failed");
        }
        evidence.delete(item);
        audit.record(tenantId, actorId, "evidence", id, "DELETE", Map.of("filename", item.getFilename()));
    }

    private String sanitize(String originalName) {
        String name = originalName == null ? "evidence" : originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N}._-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..")) name = "evidence";
        if (name.length() > 180) name = name.substring(name.length() - 180);
        return name;
    }

    public record StoredEvidence(Evidence metadata, org.springframework.core.io.Resource resource) { }
}
