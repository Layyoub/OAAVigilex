/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.TextStakeholder;
import java.util.List;
import java.util.UUID;

public interface TextStakeholderRepository extends JpaRepository<TextStakeholder, UUID> {
    List<TextStakeholder> findAllByTenantTextId(UUID tenantTextId);
    void deleteAllByTenantTextId(UUID tenantTextId);
}
