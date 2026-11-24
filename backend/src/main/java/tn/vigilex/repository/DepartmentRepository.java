/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.Department;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findAllByTenantId(UUID tenantId);
    Optional<Department> findByIdAndTenantId(UUID id, UUID tenantId);
}
