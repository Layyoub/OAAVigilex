/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.Standard;
import java.util.Optional;
import java.util.UUID;

public interface StandardRepository extends JpaRepository<Standard, UUID> {
    Optional<Standard> findByCodeIgnoreCase(String code);
}
