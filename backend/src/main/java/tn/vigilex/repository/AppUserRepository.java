/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByIdAndTenantId(UUID id, UUID tenantId);
    List<AppUser> findByTenantId(UUID tenantId);
    List<AppUser> findAllByTenantIdAndRoleIn(UUID tenantId, List<String> roles);
    boolean existsByEmailIgnoreCase(String email);
    Optional<AppUser> findFirstByRoleAndTenantIdIsNull(String role);
}
