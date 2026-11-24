/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.domain.Page;
import tn.vigilex.domain.LawText;
import java.util.List;
import java.util.UUID;

public interface LawTextSearchRepository {
    Page<LawText> search(String query, String type, String status, String authority, Integer year,
                         List<String> themes, List<String> standards, Boolean inMyBase, UUID tenantId,
                         int page, int size, String sort, String direction);
}
