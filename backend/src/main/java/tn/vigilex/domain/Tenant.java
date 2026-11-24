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
@Table(name = "tenant")
@Getter @Setter @NoArgsConstructor
public class Tenant {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String slug;
    private String logoPath;
    private String sector;
    @Column(nullable = false) private String defaultLocale = "fr";
    @Column(nullable = false) private String status = "ACTIVE";
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
