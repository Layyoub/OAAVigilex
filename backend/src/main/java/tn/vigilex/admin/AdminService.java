/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.admin;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.audit.AuditService;
import tn.vigilex.auth.AuthService;
import tn.vigilex.common.ApiException;
import tn.vigilex.domain.*;
import tn.vigilex.ingestion.ImportedLawText;
import tn.vigilex.repository.*;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class AdminService {
    private static final Set<String> TEXT_TYPES = Set.of("LAW", "DECREE", "DECREE_LAW", "ORDER", "CIRCULAR", "COMMUNIQUE", "STANDARD");
    private static final Set<String> TEXT_STATUSES = Set.of("IN_FORCE", "REPEALED", "AMENDED", "TO_VERIFY");
    private static final Set<String> ROLES = Set.of("SUPER_ADMIN", "TENANT_ADMIN", "RESP_CONFORMITE", "CONTRIBUTEUR", "LECTEUR");
    private final LawTextRepository lawTexts;
    private final LawTextVersionRepository versions;
    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final ThemeRepository themes;
    private final StandardRepository standards;
    private final OrganizationSettingsRepository organizationSettings;
    private final PasswordEncoder passwords;
    private final AuthService auth;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public AdminService(LawTextRepository lawTexts, LawTextVersionRepository versions, TenantRepository tenants,
                        AppUserRepository users, ThemeRepository themes, StandardRepository standards,
                        OrganizationSettingsRepository organizationSettings, PasswordEncoder passwords,
                        AuthService auth, AuditService audit) {
        this.lawTexts = lawTexts;
        this.versions = versions;
        this.tenants = tenants;
        this.users = users;
        this.themes = themes;
        this.standards = standards;
        this.organizationSettings = organizationSettings;
        this.passwords = passwords;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<LawText> listTexts(org.springframework.data.domain.Pageable pageable) {
        return lawTexts.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public LawText getText(UUID id) {
        return lawTexts.findById(id).orElseThrow(() -> ApiException.notFound("Library text not found"));
    }

    @Transactional
    public LawText createText(UUID actorId, TextInput input) {
        validateText(input);
        if (lawTexts.existsByReferenceIgnoreCase(input.reference().trim()))
            throw ApiException.badRequest("A text with this reference already exists");
        LawText text = new LawText();
        apply(text, input);
        text.setCreatedBy(actorId);
        LawText saved = lawTexts.save(text);
        audit.record(null, actorId, "law_text", saved.getId(), "CREATE", Map.of("reference", saved.getReference()));
        return saved;
    }

    @Transactional
    public LawText updateText(UUID actorId, UUID id, TextInput input) {
        validateText(input);
        LawText text = getText(id);
        versions.save(newVersion(text, actorId));
        if (input.reference() != null && !input.reference().equalsIgnoreCase(text.getReference())
                && lawTexts.existsByReferenceIgnoreCase(input.reference()))
            throw ApiException.badRequest("A text with this reference already exists");
        applyPartial(text, input);
        LawText saved = lawTexts.save(text);
        audit.record(null, actorId, "law_text", id, "UPDATE", Map.of("reference", saved.getReference()));
        return saved;
    }

    @Transactional
    public LawText repeal(UUID actorId, UUID id) {
        LawText text = getText(id);
        if (!"REPEALED".equals(text.getStatus())) {
            versions.save(newVersion(text, actorId));
            text.setStatus("REPEALED");
            text = lawTexts.save(text);
            audit.record(null, actorId, "law_text", id, "REPEAL", Map.of("reference", text.getReference()));
        }
        return text;
    }

    @Transactional(readOnly = true)
    public List<LawTextVersion> versions(UUID textId) {
        getText(textId);
        return versions.findAllByLawTextIdOrderByVersionNoDesc(textId);
    }

    @Transactional
    public int importTexts(UUID actorId, List<TextInput> input) {
        if (input == null || input.isEmpty() || input.size() > 5000)
            throw ApiException.badRequest("Import must contain between 1 and 5000 texts");
        Set<String> refs = new HashSet<>();
        for (TextInput record : input) {
            validateText(record);
            String reference = record.reference().trim().toLowerCase(Locale.ROOT);
            if (!refs.add(reference) || lawTexts.existsByReferenceIgnoreCase(reference))
                throw ApiException.badRequest("Import contains a duplicate reference: " + record.reference());
        }
        for (TextInput record : input) {
            LawText text = new LawText();
            apply(text, record);
            text.setCreatedBy(actorId);
            LawText saved = lawTexts.save(text);
            audit.record(null, actorId, "law_text", saved.getId(), "IMPORT", Map.of("reference", saved.getReference()));
        }
        return input.size();
    }

    @Transactional
    public int ingestTexts(UUID actorId, List<TextInput> input) {
        List<TextInput> notYetKnown = input.stream()
                .filter(item -> !lawTexts.existsByReferenceIgnoreCase(item.reference().trim())).toList();
        return notYetKnown.isEmpty() ? 0 : importTexts(actorId, notYetKnown);
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() { return tenants.findAll(); }

    @Transactional
    public Tenant createTenant(UUID actorId, TenantInput input) {
        String slug = input.slug().trim().toLowerCase(Locale.ROOT);
        if (!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || tenants.existsBySlugIgnoreCase(slug))
            throw ApiException.badRequest("Tenant slug is invalid or already used");
        Tenant tenant = new Tenant();
        tenant.setName(input.name().trim());
        tenant.setSlug(slug);
        tenant.setSector(input.sector());
        tenant.setDefaultLocale(locale(input.defaultLocale()));
        Tenant saved = tenants.save(tenant);
        OrganizationSettings settings = new OrganizationSettings();
        settings.setTenantId(saved.getId());
        settings.setOrganizationName(saved.getName());
        settings.setDefaultLocale(saved.getDefaultLocale());
        organizationSettings.save(settings);
        audit.record(null, actorId, "tenant", saved.getId(), "CREATE", Map.of("slug", slug));
        return saved;
    }

    @Transactional
    public Tenant updateTenant(UUID actorId, UUID id, TenantUpdate input) {
        Tenant tenant = tenants.findById(id).orElseThrow(() -> ApiException.notFound("Tenant not found"));
        if (input.name() != null && !input.name().isBlank()) tenant.setName(input.name().trim());
        if (input.sector() != null) tenant.setSector(input.sector());
        if (input.status() != null) {
            String status = input.status().toUpperCase(Locale.ROOT);
            if (!Set.of("ACTIVE", "SUSPENDED").contains(status)) throw ApiException.badRequest("Invalid tenant status");
            tenant.setStatus(status);
        }
        if (input.defaultLocale() != null) tenant.setDefaultLocale(locale(input.defaultLocale()));
        Tenant saved = tenants.save(tenant);
        audit.record(null, actorId, "tenant", id, "UPDATE", Map.of("status", saved.getStatus()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AppUser> listUsers(UUID tenantId) {
        return tenantId == null ? users.findAll() : users.findByTenantId(tenantId);
    }

    @Transactional
    public AppUser createUser(UUID actorId, UserInput input) {
        UUID tenantId = input.tenantId();
        if (tenantId != null && tenants.findById(tenantId).isEmpty()) throw ApiException.notFound("Tenant not found");
        String role = input.role().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(role) || (tenantId == null && !"SUPER_ADMIN".equals(role))
                || (tenantId != null && "SUPER_ADMIN".equals(role)))
            throw ApiException.badRequest("Role is not valid for this tenant");
        if (users.existsByEmailIgnoreCase(input.email())) throw ApiException.badRequest("Email is already registered");
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        AppUser user = new AppUser();
        user.setTenantId(tenantId);
        user.setEmail(input.email().trim().toLowerCase(Locale.ROOT));
        user.setFullName(input.fullName().trim());
        user.setPhone(input.phone());
        user.setRole(role);
        user.setLocale(locale(input.locale()));
        user.setPasswordHash(passwords.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(raw)));
        AppUser saved = users.save(user);
        audit.record(tenantId, actorId, "app_user", saved.getId(), "CREATE", Map.of("role", role));
        auth.requestPasswordReset(saved.getEmail());
        return saved;
    }

    @Transactional
    public AppUser updateUser(UUID actorId, UUID id, UserUpdate input) {
        AppUser user = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (input.fullName() != null && !input.fullName().isBlank()) user.setFullName(input.fullName().trim());
        if (input.phone() != null) user.setPhone(input.phone());
        if (input.locale() != null) user.setLocale(locale(input.locale()));
        if (input.role() != null) {
            String role = input.role().toUpperCase(Locale.ROOT);
            if (!ROLES.contains(role) || (user.getTenantId() == null && !"SUPER_ADMIN".equals(role))
                    || (user.getTenantId() != null && "SUPER_ADMIN".equals(role)))
                throw ApiException.badRequest("Role is not valid for this tenant");
            user.setRole(role);
        }
        if (input.active() != null) user.setActive(input.active());
        AppUser saved = users.save(user);
        audit.record(user.getTenantId(), actorId, "app_user", id, "UPDATE", Map.of("active", saved.isActive()));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Theme> listThemes() { return themes.findAll(); }

    @Transactional
    public Theme saveTheme(UUID actorId, UUID id, ThemeInput input) {
        Theme theme = id == null ? new Theme() : themes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Theme not found"));
        theme.setCode(input.code().trim().toUpperCase(Locale.ROOT));
        theme.setLabelFr(input.labelFr().trim());
        theme.setLabelEn(input.labelEn().trim());
        theme.setLabelAr(input.labelAr().trim());
        Theme saved = themes.save(theme);
        audit.record(null, actorId, "theme", saved.getId(), id == null ? "CREATE" : "UPDATE", Map.of("code", saved.getCode()));
        return saved;
    }

    @Transactional
    public void deleteTheme(UUID actorId, UUID id) {
        Theme theme = themes.findById(id).orElseThrow(() -> ApiException.notFound("Theme not found"));
        try {
            themes.delete(theme);
            audit.record(null, actorId, "theme", id, "DELETE", Map.of());
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Theme is assigned to library texts");
        }
    }

    @Transactional(readOnly = true)
    public List<Standard> listStandards() { return standards.findAll(); }

    @Transactional
    public Standard saveStandard(UUID actorId, UUID id, StandardInput input) {
        Standard standard = id == null ? new Standard() : standards.findById(id)
                .orElseThrow(() -> ApiException.notFound("Standard not found"));
        standard.setCode(input.code().trim().toUpperCase(Locale.ROOT));
        standard.setLabel(input.label().trim());
        Standard saved = standards.save(standard);
        audit.record(null, actorId, "standard", saved.getId(), id == null ? "CREATE" : "UPDATE",
                Map.of("code", saved.getCode()));
        return saved;
    }

    @Transactional
    public void deleteStandard(UUID actorId, UUID id) {
        Standard standard = standards.findById(id).orElseThrow(() -> ApiException.notFound("Standard not found"));
        try {
            standards.delete(standard);
            audit.record(null, actorId, "standard", id, "DELETE", Map.of());
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Standard is assigned to library texts");
        }
    }

    private void apply(LawText text, TextInput input) {
        text.setReference(input.reference().trim());
        text.setType(input.type().toUpperCase(Locale.ROOT));
        text.setTitleFr(input.titleFr().trim());
        text.setTitleAr(input.titleAr());
        text.setAuthority(input.authority());
        text.setPublishedOn(input.publishedOn());
        text.setOfficialGazetteRef(input.officialGazetteRef());
        text.setStatus(input.status() == null ? "IN_FORCE" : input.status().toUpperCase(Locale.ROOT));
        text.setSourceUrl(input.sourceUrl());
        text.setSummary(input.summary());
        text.setFullText(input.fullText());
        text.setThemes(resolveThemes(input.themeIds()));
        text.setStandards(resolveStandards(input.standardIds()));
    }

    private void applyPartial(LawText text, TextInput input) {
        if (input.reference() != null) text.setReference(input.reference().trim());
        if (input.type() != null) text.setType(input.type().toUpperCase(Locale.ROOT));
        if (input.titleFr() != null) text.setTitleFr(input.titleFr().trim());
        if (input.titleAr() != null) text.setTitleAr(input.titleAr());
        if (input.authority() != null) text.setAuthority(input.authority());
        if (input.publishedOn() != null) text.setPublishedOn(input.publishedOn());
        if (input.officialGazetteRef() != null) text.setOfficialGazetteRef(input.officialGazetteRef());
        if (input.status() != null) text.setStatus(input.status().toUpperCase(Locale.ROOT));
        if (input.sourceUrl() != null) text.setSourceUrl(input.sourceUrl());
        if (input.summary() != null) text.setSummary(input.summary());
        if (input.fullText() != null) text.setFullText(input.fullText());
        if (input.themeIds() != null) text.setThemes(resolveThemes(input.themeIds()));
        if (input.standardIds() != null) text.setStandards(resolveStandards(input.standardIds()));
    }

    private Set<Theme> resolveThemes(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        List<Theme> found = themes.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) throw ApiException.badRequest("Unknown theme id");
        return new HashSet<>(found);
    }

    private Set<Standard> resolveStandards(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return new HashSet<>();
        List<Standard> found = standards.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) throw ApiException.badRequest("Unknown standard id");
        return new HashSet<>(found);
    }

    private LawTextVersion newVersion(LawText text, UUID actorId) {
        LawTextVersion version = new LawTextVersion();
        version.setLawTextId(text.getId());
        version.setVersionNo(versions.findAllByLawTextIdOrderByVersionNoDesc(text.getId()).stream()
                .mapToInt(LawTextVersion::getVersionNo).max().orElse(0) + 1);
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("reference", text.getReference());
        snapshot.put("type", text.getType());
        snapshot.put("titleFr", text.getTitleFr());
        snapshot.put("titleAr", text.getTitleAr());
        snapshot.put("authority", text.getAuthority());
        snapshot.put("publishedOn", text.getPublishedOn());
        snapshot.put("status", text.getStatus());
        snapshot.put("summary", text.getSummary());
        snapshot.put("fullText", text.getFullText());
        version.setSnapshot(snapshot);
        version.setChangedBy(actorId);
        return version;
    }

    private void validateText(TextInput input) {
        if (input == null || blank(input.reference()) || blank(input.type()) || blank(input.titleFr()))
            throw ApiException.badRequest("Reference, type and French title are required");
        if (input.reference().length() > 120 || input.titleFr().length() > 500)
            throw ApiException.badRequest("Text reference or title is too long");
        if (!TEXT_TYPES.contains(input.type().toUpperCase(Locale.ROOT))) throw ApiException.badRequest("Unsupported legal text type");
        if (input.status() != null && !TEXT_STATUSES.contains(input.status().toUpperCase(Locale.ROOT)))
            throw ApiException.badRequest("Unsupported legal text status");
        if (input.sourceUrl() != null && !input.sourceUrl().isBlank()) {
            try {
                URI uri = URI.create(input.sourceUrl());
                if (!Set.of("https", "http").contains(uri.getScheme()) || uri.getHost() == null)
                    throw ApiException.badRequest("Source URL must use HTTP or HTTPS");
            } catch (IllegalArgumentException e) {
                throw ApiException.badRequest("Source URL is invalid");
            }
        }
    }

    private String locale(String value) {
        String locale = value == null || value.isBlank() ? "fr" : value.toLowerCase(Locale.ROOT);
        if (!Set.of("fr", "en", "ar").contains(locale)) throw ApiException.badRequest("Locale must be fr, en or ar");
        return locale;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    public record TextInput(String reference, String type, String titleFr, String titleAr, String authority,
                            LocalDate publishedOn, String officialGazetteRef, String status, String sourceUrl,
                            String summary, String fullText, List<UUID> themeIds, List<UUID> standardIds) {
        public static TextInput from(ImportedLawText item) {
            return new TextInput(item.reference(), item.type(), item.titleFr(), item.titleAr(), item.authority(),
                    item.publishedOn(), item.officialGazetteRef(), item.status(), item.sourceUrl(), item.summary(),
                    item.fullText(), List.of(), List.of());
        }
    }
    public record TenantInput(String name, String slug, String sector, String defaultLocale) { }
    public record TenantUpdate(String name, String sector, String status, String defaultLocale) { }
    public record UserInput(UUID tenantId, String email, String fullName, String phone, String role, String locale) { }
    public record UserUpdate(String fullName, String phone, String role, String locale, Boolean active) { }
    public record ThemeInput(String code, String labelFr, String labelEn, String labelAr) { }
    public record StandardInput(String code, String label) { }
}
