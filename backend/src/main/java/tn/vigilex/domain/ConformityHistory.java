/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conformity_history")
@Getter @Setter @NoArgsConstructor
public class ConformityHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID tenantTextId;
    private String fromStatus;
    @Column(nullable = false) private String toStatus;
    @Column(nullable = false) private UUID changedBy;
    @Column(nullable = false) private Instant changedAt = Instant.now();
    @Column(columnDefinition = "text") private String note;
}
