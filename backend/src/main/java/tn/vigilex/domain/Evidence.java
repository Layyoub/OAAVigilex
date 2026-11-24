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
@Table(name = "evidence")
@Getter @Setter @NoArgsConstructor
public class Evidence {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantId;
    private UUID tenantTextId;
    private UUID actionId;
    @Column(nullable = false) private String filename;
    @Column(nullable = false, unique = true) private String storageKey;
    @Column(nullable = false) private String mime;
    @Column(nullable = false) private long size;
    @Column(nullable = false) private UUID uploadedBy;
    @Column(nullable = false) private Instant uploadedAt = Instant.now();
}
