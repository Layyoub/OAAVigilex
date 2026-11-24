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
@Table(name = "organization_settings")
@Getter @Setter @NoArgsConstructor
public class OrganizationSettings {
    @Id private UUID tenantId;
    @Column(nullable = false) private String organizationName;
    @Column(nullable = false) private String defaultLocale = "fr";
    @Column(nullable = false) private Instant updatedAt = Instant.now();
}
