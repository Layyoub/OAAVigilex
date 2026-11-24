/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.vigilex.common.ApiException;
import tn.vigilex.common.PageResponse;
import tn.vigilex.domain.*;
import tn.vigilex.ingestion.ImportedLawText;
import tn.vigilex.ingestion.IngestionScheduler;
import tn.vigilex.ingestion.ManualCsvSourceAdapter;
import tn.vigilex.security.UserPrincipal;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {
    private final AdminService service;
    private final ManualCsvSourceAdapter csv;
    private final IngestionScheduler ingestion;
    private final ObjectMapper objectMapper;

    public AdminController(AdminService service, ManualCsvSourceAdapter csv,
                           IngestionScheduler ingestion, ObjectMapper objectMapper) {
        this.service = service;
        this.csv = csv;
        this.ingestion = ingestion;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/texts")
    public PageResponse<LawText> listTexts(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "25") int size) {
        validatePage(page, size);
        return PageResponse.from(service.listTexts(PageRequest.of(page, size)));
    }
    @PostMapping("/texts")
    public LawText createText(@Valid @RequestBody TextRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        return service.createText(actor.id(), request.toInput());
    }
    @PutMapping("/texts/{id}")
    public LawText updateText(@PathVariable UUID id, @RequestBody TextRequest request,
                              @AuthenticationPrincipal UserPrincipal actor) {
        return service.updateText(actor.id(), id, request.toInput());
    }
    @PatchMapping("/texts/{id}/repeal")
    public LawText repeal(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal actor) {
        return service.repeal(actor.id(), id);
    }
    @GetMapping("/texts/{id}/versions")
    public List<LawTextVersion> versions(@PathVariable UUID id) { return service.versions(id); }

    @PostMapping(value = "/texts/import", consumes = "text/csv")
    public ImportResult importCsv(@RequestBody @Size(max = 10_000_000) String content,
                                  @AuthenticationPrincipal UserPrincipal actor) {
        List<AdminService.TextInput> records = csv.parse(content).stream().map(AdminService.TextInput::from).toList();
        return new ImportResult(service.importTexts(actor.id(), records));
    }
    @PostMapping(value = "/texts/import/json", consumes = "application/json")
    public ImportResult importJson(@RequestBody String content, @AuthenticationPrincipal UserPrincipal actor) {
        try {
            List<AdminService.TextInput> records = objectMapper.readValue(content, new TypeReference<>() { });
            return new ImportResult(service.importTexts(actor.id(), records));
        } catch (IOException e) {
            throw ApiException.badRequest("Invalid JSON import document");
        }
    }

    @GetMapping("/tenants")
    public List<Tenant> tenants() { return service.listTenants(); }
    @PostMapping("/tenants")
    public Tenant createTenant(@Valid @RequestBody TenantRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        return service.createTenant(actor.id(), new AdminService.TenantInput(
                request.name(), request.slug(), request.sector(), request.defaultLocale()));
    }
    @PatchMapping("/tenants/{id}")
    public Tenant updateTenant(@PathVariable UUID id, @RequestBody AdminService.TenantUpdate request,
                               @AuthenticationPrincipal UserPrincipal actor) {
        return service.updateTenant(actor.id(), id, request);
    }

    @GetMapping("/users")
    public List<UserView> users(@RequestParam(required = false) UUID tenantId) {
        return service.listUsers(tenantId).stream().map(UserView::from).toList();
    }
    @PostMapping("/users")
    public UserView createUser(@Valid @RequestBody UserRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        AppUser user = service.createUser(actor.id(), new AdminService.UserInput(request.tenantId(), request.email(),
                request.fullName(), request.phone(), request.role(), request.locale()));
        return UserView.from(user);
    }
    @PatchMapping("/users/{id}")
    public UserView updateUser(@PathVariable UUID id, @RequestBody AdminService.UserUpdate request,
                               @AuthenticationPrincipal UserPrincipal actor) {
        return UserView.from(service.updateUser(actor.id(), id, request));
    }

    @GetMapping("/themes")
    public List<Theme> themes() { return service.listThemes(); }
    @PostMapping("/themes")
    public Theme createTheme(@Valid @RequestBody ThemeRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        return service.saveTheme(actor.id(), null, new AdminService.ThemeInput(
                request.code(), request.labelFr(), request.labelEn(), request.labelAr()));
    }
    @PutMapping("/themes/{id}")
    public Theme updateTheme(@PathVariable UUID id, @Valid @RequestBody ThemeRequest request,
                             @AuthenticationPrincipal UserPrincipal actor) {
        return service.saveTheme(actor.id(), id, new AdminService.ThemeInput(
                request.code(), request.labelFr(), request.labelEn(), request.labelAr()));
    }
    @DeleteMapping("/themes/{id}")
    public void deleteTheme(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal actor) {
        service.deleteTheme(actor.id(), id);
    }

    @GetMapping("/standards")
    public List<Standard> standards() { return service.listStandards(); }
    @PostMapping("/standards")
    public Standard createStandard(@Valid @RequestBody StandardRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        return service.saveStandard(actor.id(), null, new AdminService.StandardInput(request.code(), request.label()));
    }
    @PutMapping("/standards/{id}")
    public Standard updateStandard(@PathVariable UUID id, @Valid @RequestBody StandardRequest request,
                                   @AuthenticationPrincipal UserPrincipal actor) {
        return service.saveStandard(actor.id(), id, new AdminService.StandardInput(request.code(), request.label()));
    }
    @DeleteMapping("/standards/{id}")
    public void deleteStandard(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal actor) {
        service.deleteStandard(actor.id(), id);
    }

    @GetMapping("/ingestion/adapters")
    public List<String> adapters() { return ingestion.availableAdapters(); }
    @PostMapping("/ingestion/run")
    public ImportResult runIngestion(@AuthenticationPrincipal UserPrincipal actor) {
        return new ImportResult(ingestion.runNow(actor.id()));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw ApiException.badRequest("Invalid pagination bounds");
    }

    public record ImportResult(int imported) { }
    public record TextRequest(@NotBlank @Size(max = 120) String reference, @NotBlank String type,
                              @NotBlank @Size(max = 500) String titleFr, String titleAr, String authority,
                              java.time.LocalDate publishedOn, String officialGazetteRef, String status,
                              String sourceUrl, String summary, String fullText, List<UUID> themeIds,
                              List<UUID> standardIds) {
        AdminService.TextInput toInput() {
            return new AdminService.TextInput(reference, type, titleFr, titleAr, authority, publishedOn,
                    officialGazetteRef, status, sourceUrl, summary, fullText, themeIds, standardIds);
        }
    }
    public record TenantRequest(@NotBlank @Size(max = 180) String name, @NotBlank @Size(max = 90) String slug,
                                String sector, String defaultLocale) { }
    public record UserRequest(UUID tenantId, @Email @NotBlank String email, @NotBlank String fullName,
                              String phone, @NotBlank String role, String locale) { }
    public record ThemeRequest(@NotBlank String code, @NotBlank String labelFr,
                               @NotBlank String labelEn, @NotBlank String labelAr) { }
    public record StandardRequest(@NotBlank String code, @NotBlank String label) { }
    public record UserView(UUID id, UUID tenantId, String email, String fullName, String phone, String role,
                           String locale, boolean active) {
        static UserView from(AppUser user) {
            return new UserView(user.getId(), user.getTenantId(), user.getEmail(), user.getFullName(), user.getPhone(),
                    user.getRole(), user.getLocale(), user.isActive());
        }
    }
}
