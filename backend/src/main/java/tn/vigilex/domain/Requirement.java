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
@Table(name = "requirement")
@Getter @Setter @NoArgsConstructor
public class Requirement {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantTextId;
    private String articleRef;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Column(nullable = false) private String status = "NOT_ANALYZED";
}
