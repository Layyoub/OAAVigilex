/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tn.vigilex.domain.LawText;
import java.util.UUID;

public interface LawTextRepository extends JpaRepository<LawText, UUID>, JpaSpecificationExecutor<LawText>,
        LawTextSearchRepository {
    Page<LawText> findByTitleFrContainingIgnoreCaseOrTitleArContainingIgnoreCaseOrReferenceContainingIgnoreCase(
            String french, String arabic, String reference, Pageable pageable);
    boolean existsByReferenceIgnoreCase(String reference);
}
