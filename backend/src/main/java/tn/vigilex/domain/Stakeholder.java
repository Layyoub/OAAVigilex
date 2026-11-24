/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "stakeholder")
@Getter @Setter @NoArgsConstructor
public class Stakeholder {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String type;
}
