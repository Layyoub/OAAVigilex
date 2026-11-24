/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Set;

@Entity
@Table(name = "law_text")
@Getter @Setter @NoArgsConstructor
public class LawText {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true) private String reference;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String titleFr;
    private String titleAr;
    private String authority;
    private LocalDate publishedOn;
    private String officialGazetteRef;
    @Column(nullable = false) private String status = "IN_FORCE";
    private String sourceUrl;
    @Column(columnDefinition = "text") private String summary;
    @Column(columnDefinition = "text") private String fullText;
    @Column(nullable = false) private Instant addedAt = Instant.now();
    private UUID createdBy;
    @ManyToMany
    @JoinTable(name = "law_text_theme", joinColumns = @JoinColumn(name = "law_text_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id"))
    private Set<Theme> themes = new java.util.HashSet<>();
    @ManyToMany
    @JoinTable(name = "law_text_standard", joinColumns = @JoinColumn(name = "law_text_id"),
            inverseJoinColumns = @JoinColumn(name = "standard_id"))
    private Set<Standard> standards = new java.util.HashSet<>();

    @Transient
    public boolean isNew() {
        return addedAt != null && addedAt.isAfter(Instant.now().minus(java.time.Duration.ofDays(14)));
    }
}
