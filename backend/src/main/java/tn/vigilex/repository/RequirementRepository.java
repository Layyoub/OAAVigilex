/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.Requirement;
import java.util.List;
import java.util.UUID;

public interface RequirementRepository extends JpaRepository<Requirement, UUID> {
    List<Requirement> findAllByTenantTextId(UUID tenantTextId);
}
