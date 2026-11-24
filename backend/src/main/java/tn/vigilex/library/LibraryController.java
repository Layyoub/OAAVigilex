/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.library;

import org.springframework.web.bind.annotation.*;
import tn.vigilex.common.ApiException;
import tn.vigilex.common.PageResponse;
import tn.vigilex.domain.LawText;
import tn.vigilex.repository.LawTextRepository;
import tn.vigilex.tenant.TenantContext;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.util.Map;
import tn.vigilex.repository.ThemeRepository;
import tn.vigilex.repository.StandardRepository;
import tn.vigilex.domain.Theme;
import tn.vigilex.domain.Standard;

@RestController
@RequestMapping("/api/v1/library/texts")
public class LibraryController {
    private static final List<String> SORT_FIELDS = List.of("addedAt", "publishedOn", "reference", "titleFr");
    private final LawTextRepository texts;
    private final ThemeRepository themes;
    private final StandardRepository standards;
    public LibraryController(LawTextRepository texts, ThemeRepository themes, StandardRepository standards) {
        this.texts = texts;
        this.themes = themes;
        this.standards = standards;
    }

    @GetMapping
    public PageResponse<LawText> search(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String authority,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) List<String> theme,
                                        @RequestParam(required = false) List<String> standard,
                                        @RequestParam(required = false) Boolean inMyBase,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "25") int size,
                                        @RequestParam(defaultValue = "addedAt") String sort,
                                        @RequestParam(defaultValue = "desc") String direction) {
        if (page < 0 || size < 1 || size > 100 || (year != null && (year < 1 || year > 9998)))
            throw ApiException.badRequest("Invalid search or pagination bounds");
        if (!SORT_FIELDS.contains(sort)) throw ApiException.badRequest("Unsupported sort field");
        UUID tenantId = inMyBase == null ? null : TenantContext.requireTenantId();
        var result = texts.search(q, type, status, authority, year, theme, standard, inMyBase, tenantId,
                page, size, sort, direction);
        return PageResponse.from(result);
    }

    @GetMapping("/facets")
    public Map<String, Object> facets() {
        List<LawText> all = texts.findAll();
        return Map.of(
                "types", all.stream().map(LawText::getType).distinct().sorted().toList(),
                "statuses", all.stream().map(LawText::getStatus).distinct().sorted().toList(),
                "authorities", all.stream().map(LawText::getAuthority).filter(java.util.Objects::nonNull).distinct().sorted().toList(),
                "years", all.stream().map(LawText::getPublishedOn).filter(java.util.Objects::nonNull).map(LocalDate::getYear).distinct().sorted().toList(),
                "themes", themes.findAll().stream().map(Theme::getCode).sorted().toList(),
                "standards", standards.findAll().stream().map(Standard::getCode).sorted().toList());
    }

    @GetMapping("/{id}")
    public LawText get(@PathVariable UUID id) {
        return texts.findById(id).orElseThrow(() -> ApiException.notFound("Library text not found"));
    }
}
