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
@Table(name = "action")
@Getter @Setter @NoArgsConstructor
public class ActionItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private UUID tenantTextId;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String description;
    private UUID responsibleId;
    private String responsiblePhone;
    @Column(nullable = false) private String priority = "MEDIUM";
    private LocalDate dueOn;
    @Column(nullable = false) private String status = "TO_DO";
    private LocalDate doneOn;
    @Column(nullable = false) private String effectiveness = "UNDEFINED";
    @Column(columnDefinition = "text") private String effectivenessComment;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
