/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;

@Entity
@Table(name = "law_text_version", uniqueConstraints = @UniqueConstraint(columnNames = {"law_text_id", "version_no"}))
@Getter @Setter @NoArgsConstructor
public class LawTextVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID lawTextId;
    @Column(nullable = false) private int versionNo;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Object> snapshot;
    @Column(nullable = false) private Instant changedAt = Instant.now();
    private UUID changedBy;
}
