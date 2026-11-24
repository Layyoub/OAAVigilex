/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.ComplianceSnapshot;
import java.util.List;
import java.util.UUID;

public interface ComplianceSnapshotRepository extends JpaRepository<ComplianceSnapshot, UUID> {
    List<ComplianceSnapshot> findTop12ByTenantIdOrderByTakenOnDesc(UUID tenantId);
    java.util.Optional<ComplianceSnapshot> findByTenantIdAndTakenOn(UUID tenantId, java.time.LocalDate takenOn);
}
