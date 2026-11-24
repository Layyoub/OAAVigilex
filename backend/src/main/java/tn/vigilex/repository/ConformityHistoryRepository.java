/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.ConformityHistory;
import java.util.List;
import java.util.UUID;

public interface ConformityHistoryRepository extends JpaRepository<ConformityHistory, UUID> {
    List<ConformityHistory> findAllByTenantIdAndTenantTextIdOrderByChangedAtDesc(UUID tenantId, UUID tenantTextId);
}
