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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "compliance_snapshot")
@Getter @Setter @NoArgsConstructor
public class ComplianceSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private LocalDate takenOn = LocalDate.now();
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal score;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Object> counts = new HashMap<>();
}
