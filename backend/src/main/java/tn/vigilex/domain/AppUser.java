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
@Table(name = "app_user")
@Getter @Setter @NoArgsConstructor
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID tenantId;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;
    @Column(nullable = false) private String fullName;
    private String phone;
    @Column(nullable = false) private String role;
    @Column(nullable = false) private String locale = "fr";
    @Column(nullable = false) private String theme = "system";
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private int failedAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
