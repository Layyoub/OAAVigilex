/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.SavedView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedViewRepository extends JpaRepository<SavedView, UUID> {
    List<SavedView> findAllByUserId(UUID userId);
    Optional<SavedView> findByIdAndUserId(UUID id, UUID userId);
}
