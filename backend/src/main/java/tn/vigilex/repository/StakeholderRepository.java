/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.Stakeholder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StakeholderRepository extends JpaRepository<Stakeholder, UUID> {
    List<Stakeholder> findAllByTenantId(UUID tenantId);
    Optional<Stakeholder> findByIdAndTenantId(UUID id, UUID tenantId);
}
