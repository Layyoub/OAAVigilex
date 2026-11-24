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
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "saved_view")
@Getter @Setter @NoArgsConstructor
public class SavedView {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false) private String scope;
    @Column(nullable = false) private String name;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Object> filters = new HashMap<>();
}
