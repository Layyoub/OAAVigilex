/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tenant_text", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "law_text_id"}))
@Getter @Setter @NoArgsConstructor
public class TenantText {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID lawTextId;
    @Column(nullable = false) private String applicability = "TO_REVIEW";
    @Column(columnDefinition = "text") private String applicabilityReason;
    @Column(nullable = false) private String conformity = "NOT_ANALYZED";
    private LocalDate verifiedOn;
    @Column(columnDefinition = "text") private String currentSituation;
    @Column(columnDefinition = "text") private String comment;
    private UUID ownerId;
    private LocalDate nextReviewOn;
    @Column(nullable = false) private Instant updatedAt = Instant.now();
}
