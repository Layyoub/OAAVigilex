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
@Table(name = "password_reset_token")
@Getter @Setter @NoArgsConstructor
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false, unique = true) private String tokenHash;
    @Column(nullable = false) private Instant expiresAt;
    private Instant usedAt;
}
