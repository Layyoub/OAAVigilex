/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.vigilex.domain.TenantText;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantTextRepository extends JpaRepository<TenantText, UUID> {
    Page<TenantText> findAllByTenantId(UUID tenantId, Pageable pageable);
    List<TenantText> findAllByTenantId(UUID tenantId);
    Optional<TenantText> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<TenantText> findByTenantIdAndLawTextId(UUID tenantId, UUID lawTextId);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndConformity(UUID tenantId, String conformity);
    long countByTenantIdAndConformityNot(UUID tenantId, String conformity);
    @Query("select t.conformity, count(t) from TenantText t where t.tenantId = :tenantId group by t.conformity")
    List<Object[]> countConformityByTenant(@Param("tenantId") UUID tenantId);
}
