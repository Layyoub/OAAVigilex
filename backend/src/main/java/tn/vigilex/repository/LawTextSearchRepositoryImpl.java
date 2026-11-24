/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import tn.vigilex.domain.LawText;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class LawTextSearchRepositoryImpl implements LawTextSearchRepository {
    private static final java.util.Map<String, String> SORT_COLUMNS = java.util.Map.of(
            "addedAt", "added_at", "publishedOn", "published_on", "reference", "reference", "titleFr", "title_fr");
    private final EntityManager entityManager;
    public LawTextSearchRepositoryImpl(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public Page<LawText> search(String queryText, String type, String status, String authority, Integer year,
                               List<String> themes, List<String> standards, Boolean inMyBase, UUID tenantId,
                               int page, int size, String sort, String direction) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        if (queryText != null && !queryText.isBlank()) {
            where.append(" AND (search_vector @@ plainto_tsquery('french', unaccent(:query)) ")
                    .append(" OR search_vector @@ plainto_tsquery('simple', :query) ")
                    .append(" OR reference ILIKE '%' || :query || '%') ");
            parameters.put("query", queryText.trim());
        }
        addEquals(where, parameters, "type", "type", type);
        addEquals(where, parameters, "status", "status", status);
        if (authority != null && !authority.isBlank()) {
            where.append(" AND authority ILIKE '%' || :authority || '%' ");
            parameters.put("authority", authority.trim());
        }
        if (year != null) {
            where.append(" AND published_on >= :yearStart AND published_on < :yearEnd ");
            parameters.put("yearStart", LocalDate.of(year, 1, 1));
            parameters.put("yearEnd", LocalDate.of(year, 1, 1).plusYears(1));
        }
        addInClause(where, parameters, "law_text_theme", "theme_id", "theme", themes,
                "theme", "code");
        addInClause(where, parameters, "law_text_standard", "standard_id", "standard", standards,
                "standard", "code");
        if (inMyBase != null) {
            if (tenantId == null) throw tn.vigilex.common.ApiException.forbidden();
            where.append(inMyBase ? " AND " : " AND NOT ")
                    .append("EXISTS (SELECT 1 FROM tenant_text tt WHERE tt.law_text_id = law_text.id AND tt.tenant_id = :tenantId) ");
            parameters.put("tenantId", tenantId);
        }
        String sortColumn = SORT_COLUMNS.getOrDefault(sort, "added_at");
        String sortDirection = "asc".equalsIgnoreCase(direction) ? "ASC" : "DESC";
        String from = " FROM law_text " + where;
        Query countQuery = entityManager.createNativeQuery("SELECT count(*)" + from);
        Query select = entityManager.createNativeQuery("SELECT law_text.*" + from + " ORDER BY " + sortColumn
                + " " + sortDirection + " LIMIT :limit OFFSET :offset", LawText.class);
        parameters.forEach((key, value) -> { countQuery.setParameter(key, value); select.setParameter(key, value); });
        select.setParameter("limit", size);
        select.setParameter("offset", page * size);
        @SuppressWarnings("unchecked")
        List<LawText> content = select.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private void addEquals(StringBuilder where, java.util.Map<String, Object> parameters,
                           String column, String parameter, String value) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(column).append(" = :").append(parameter).append(" ");
            parameters.put(parameter, value.toUpperCase(java.util.Locale.ROOT));
        }
    }

    private void addInClause(StringBuilder where, java.util.Map<String, Object> parameters, String joinTable,
                             String foreignKey, String prefix, List<String> values,
                             String entityTable, String entityCode) {
        if (values == null || values.isEmpty()) return;
        where.append(" AND EXISTS (SELECT 1 FROM ").append(joinTable).append(" ")
                .append(prefix).append("_link JOIN ").append(entityTable).append(" ").append(prefix)
                .append(" ON ").append(prefix).append("_link.").append(foreignKey).append(" = ")
                .append(prefix).append(".id WHERE ").append(prefix).append("_link.law_text_id = law_text.id")
                .append(" AND ").append(prefix).append(".").append(entityCode).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            String parameter = prefix + i;
            if (i > 0) where.append(',');
            where.append(':').append(parameter);
            parameters.put(parameter, values.get(i).toUpperCase(java.util.Locale.ROOT));
        }
        where.append(")) ");
    }
}
