/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.LawTextVersion;
import java.util.List;
import java.util.UUID;

public interface LawTextVersionRepository extends JpaRepository<LawTextVersion, UUID> {
    List<LawTextVersion> findAllByLawTextIdOrderByVersionNoDesc(UUID lawTextId);
}
