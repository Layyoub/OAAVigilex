/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.vigilex.domain.ActionItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionRepository extends JpaRepository<ActionItem, UUID>, JpaSpecificationExecutor<ActionItem> {
    Page<ActionItem> findAllByTenantId(UUID tenantId, Pageable pageable);
    List<ActionItem> findAllByTenantId(UUID tenantId);
    Page<ActionItem> findAllByTenantIdAndResponsibleId(UUID tenantId, UUID responsibleId, Pageable pageable);
    List<ActionItem> findAllByTenantIdAndTenantTextId(UUID tenantId, UUID tenantTextId);
    Optional<ActionItem> findByIdAndTenantId(UUID id, UUID tenantId);
    List<ActionItem> findAllByTenantIdAndDueOnBeforeAndStatusNot(UUID tenantId, LocalDate date, String status);
    List<ActionItem> findAllByDueOnAndStatusNot(LocalDate date, String status);
    List<ActionItem> findAllByDueOnBeforeAndStatusNot(LocalDate date, String status);
    long countByTenantIdAndDueOnBeforeAndStatusNot(UUID tenantId, LocalDate date, String status);
}
