/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.domain.AuditLog;
import tn.vigilex.repository.AuditLogRepository;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID tenantId, UUID userId, String entity, UUID entityId, String operation, Map<String, ?> diff) {
        AuditLog log = new AuditLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setOperation(operation);
        log.setDiff(new java.util.LinkedHashMap<>(diff));
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(UUID tenantId, Pageable pageable) {
        return repository.findAllByTenantIdOrderByAtDesc(tenantId, pageable);
    }
}
