/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.Evidence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
    Optional<Evidence> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Evidence> findAllByTenantIdAndTenantTextId(UUID tenantId, UUID tenantTextId);
    List<Evidence> findAllByTenantIdAndActionId(UUID tenantId, UUID actionId);
}
