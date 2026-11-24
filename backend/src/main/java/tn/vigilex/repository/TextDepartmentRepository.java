/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.TextDepartment;
import java.util.List;
import java.util.UUID;

public interface TextDepartmentRepository extends JpaRepository<TextDepartment, UUID> {
    List<TextDepartment> findAllByTenantTextId(UUID tenantTextId);
    List<TextDepartment> findAllByDepartmentIdIn(List<UUID> departmentIds);
    void deleteAllByTenantTextId(UUID tenantTextId);
}
