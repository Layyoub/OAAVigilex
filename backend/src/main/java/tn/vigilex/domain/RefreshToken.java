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
@Table(name = "refresh_token")
@Getter @Setter @NoArgsConstructor
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID userId;
    @Column(nullable = false, unique = true) private String tokenHash;
    @Column(nullable = false) private Instant expiresAt;
    private Instant revokedAt;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
