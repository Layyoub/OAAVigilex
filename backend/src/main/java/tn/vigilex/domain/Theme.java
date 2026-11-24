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
@Table(name = "theme")
@Getter @Setter @NoArgsConstructor
public class Theme {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true) private String code;
    @Column(nullable = false) private String labelFr;
    @Column(nullable = false) private String labelEn;
    @Column(nullable = false) private String labelAr;
}
