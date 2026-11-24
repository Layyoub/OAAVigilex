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
@Table(name = "action_reminder", uniqueConstraints = @UniqueConstraint(columnNames = {"action_id", "reminder_date", "reminder_type"}))
@Getter @Setter @NoArgsConstructor
public class ActionReminder {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID actionId;
    @Column(nullable = false) private LocalDate reminderDate;
    @Column(nullable = false) private String reminderType;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
