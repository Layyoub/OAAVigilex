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
@Table(name = "tenant_text_department", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_text_id", "department_id"}))
@Getter @Setter @NoArgsConstructor
public class TextDepartment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantTextId;
    @Column(nullable = false) private UUID departmentId;
}
