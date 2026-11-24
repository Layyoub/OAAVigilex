/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.organization;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.audit.AuditService;
import tn.vigilex.auth.AuthService;
import tn.vigilex.common.ApiException;
import tn.vigilex.domain.*;
import tn.vigilex.repository.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrganizationService {
    private static final Set<String> TENANT_ROLES = Set.of("TENANT_ADMIN", "RESP_CONFORMITE", "CONTRIBUTEUR", "LECTEUR");
    private static final Set<String> LOCALES = Set.of("fr", "en", "ar");
    private final AppUserRepository users;
    private final DepartmentRepository departments;
    private final StakeholderRepository stakeholders;
    private final TenantRepository tenants;
    private final OrganizationSettingsRepository settings;
    private final PasswordEncoder passwords;
    private final AuthService auth;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public OrganizationService(AppUserRepository users, DepartmentRepository departments,
                               StakeholderRepository stakeholders, TenantRepository tenants,
                               OrganizationSettingsRepository settings, PasswordEncoder passwords,
                               AuthService auth, AuditService audit) {
        this.users = users;
        this.departments = departments;
        this.stakeholders = stakeholders;
        this.tenants = tenants;
        this.settings = settings;
        this.passwords = passwords;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<AppUser> users(UUID tenantId) { return users.findByTenantId(tenantId); }

    @Transactional
    public AppUser createUser(UUID tenantId, UUID actorId, CreateUser request) {
        if (users.existsByEmailIgnoreCase(request.email())) throw ApiException.badRequest("Email is already registered");
        String role = request.role().toUpperCase(Locale.ROOT);
        if (!TENANT_ROLES.contains(role)) throw ApiException.badRequest("Unsupported organization role");
        String locale = normalizeLocale(request.locale());
        AppUser user = new AppUser();
        user.setTenantId(tenantId);
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setRole(role);
        user.setLocale(locale);
        byte[] randomPassword = new byte[32];
        random.nextBytes(randomPassword);
        user.setPasswordHash(passwords.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(randomPassword)));
        AppUser saved = users.save(user);
        audit.record(tenantId, actorId, "app_user", saved.getId(), "CREATE", Map.of("role", role));
        auth.requestPasswordReset(saved.getEmail());
        return saved;
    }

    @Transactional
    public AppUser updateUser(UUID tenantId, UUID actorId, UUID id, UpdateUser request) {
        AppUser user = users.findByIdAndTenantId(id, tenantId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (request.fullName() != null && !request.fullName().isBlank()) user.setFullName(request.fullName().trim());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.locale() != null) user.setLocale(normalizeLocale(request.locale()));
        if (request.role() != null) {
            String role = request.role().toUpperCase(Locale.ROOT);
            if (!TENANT_ROLES.contains(role)) throw ApiException.badRequest("Unsupported organization role");
            user.setRole(role);
        }
        if (request.active() != null) user.setActive(request.active());
        AppUser saved = users.save(user);
        audit.record(tenantId, actorId, "app_user", id, "UPDATE", Map.of("active", saved.isActive(), "role", saved.getRole()));
        return saved;
    }

    @Transactional(readOnly = true)
    public AppUser profile(UUID userId, UUID tenantId) {
        return users.findById(userId).filter(user -> java.util.Objects.equals(user.getTenantId(), tenantId))
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional
    public AppUser updateProfile(UUID userId, UUID tenantId, UpdateProfile request) {
        AppUser user = profile(userId, tenantId);
        if (request.fullName() != null && !request.fullName().isBlank()) user.setFullName(request.fullName().trim());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.locale() != null) user.setLocale(normalizeLocale(request.locale()));
        if (request.theme() != null) {
            if (!Set.of("light", "dark", "system").contains(request.theme().toLowerCase(Locale.ROOT)))
                throw ApiException.badRequest("Unsupported theme");
            user.setTheme(request.theme().toLowerCase(Locale.ROOT));
        }
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public List<Department> departments(UUID tenantId) { return departments.findAllByTenantId(tenantId); }

    @Transactional
    public Department createDepartment(UUID tenantId, UUID actorId, DepartmentRequest request) {
        Department item = new Department();
        item.setTenantId(tenantId);
        item.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        item.setName(request.name().trim());
        Department saved = departments.save(item);
        audit.record(tenantId, actorId, "department", saved.getId(), "CREATE", Map.of("code", saved.getCode()));
        return saved;
    }

    @Transactional
    public Department updateDepartment(UUID tenantId, UUID actorId, UUID id, DepartmentRequest request) {
        Department item = departments.findByIdAndTenantId(id, tenantId).orElseThrow(() -> ApiException.notFound("Department not found"));
        item.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        item.setName(request.name().trim());
        Department saved = departments.save(item);
        audit.record(tenantId, actorId, "department", id, "UPDATE", Map.of("code", saved.getCode()));
        return saved;
    }

    @Transactional
    public void deleteDepartment(UUID tenantId, UUID actorId, UUID id) {
        Department item = departments.findByIdAndTenantId(id, tenantId).orElseThrow(() -> ApiException.notFound("Department not found"));
        departments.delete(item);
        audit.record(tenantId, actorId, "department", id, "DELETE", Map.of());
    }

    @Transactional(readOnly = true)
    public List<Stakeholder> stakeholders(UUID tenantId) { return stakeholders.findAllByTenantId(tenantId); }

    @Transactional
    public Stakeholder createStakeholder(UUID tenantId, UUID actorId, StakeholderRequest request) {
        Stakeholder item = new Stakeholder();
        item.setTenantId(tenantId);
        item.setName(request.name().trim());
        item.setType(request.type().trim());
        Stakeholder saved = stakeholders.save(item);
        audit.record(tenantId, actorId, "stakeholder", saved.getId(), "CREATE", Map.of("name", saved.getName()));
        return saved;
    }

    @Transactional
    public Stakeholder updateStakeholder(UUID tenantId, UUID actorId, UUID id, StakeholderRequest request) {
        Stakeholder item = stakeholders.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Stakeholder not found"));
        item.setName(request.name().trim());
        item.setType(request.type().trim());
        Stakeholder saved = stakeholders.save(item);
        audit.record(tenantId, actorId, "stakeholder", id, "UPDATE", Map.of("name", saved.getName()));
        return saved;
    }

    @Transactional
    public void deleteStakeholder(UUID tenantId, UUID actorId, UUID id) {
        Stakeholder item = stakeholders.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("Stakeholder not found"));
        stakeholders.delete(item);
        audit.record(tenantId, actorId, "stakeholder", id, "DELETE", Map.of());
    }

    @Transactional(readOnly = true)
    public OrganizationSettings settings(UUID tenantId) {
        return settings.findById(tenantId).orElseGet(() -> {
            Tenant tenant = tenants.findById(tenantId).orElseThrow(() -> ApiException.notFound("Organization not found"));
            OrganizationSettings defaults = new OrganizationSettings();
            defaults.setTenantId(tenantId);
            defaults.setOrganizationName(tenant.getName());
            defaults.setDefaultLocale(tenant.getDefaultLocale());
            return defaults;
        });
    }

    @Transactional
    public OrganizationSettings updateSettings(UUID tenantId, UUID actorId, SettingsRequest request) {
        Tenant tenant = tenants.findById(tenantId).orElseThrow(() -> ApiException.notFound("Organization not found"));
        OrganizationSettings item = settings.findById(tenantId).orElseGet(() -> {
            OrganizationSettings created = new OrganizationSettings();
            created.setTenantId(tenantId);
            created.setOrganizationName(tenant.getName());
            return created;
        });
        if (request.organizationName() != null && !request.organizationName().isBlank()) {
            item.setOrganizationName(request.organizationName().trim());
            tenant.setName(request.organizationName().trim());
        }
        if (request.defaultLocale() != null) {
            String locale = normalizeLocale(request.defaultLocale());
            item.setDefaultLocale(locale);
            tenant.setDefaultLocale(locale);
        }
        if (request.logoPath() != null) tenant.setLogoPath(request.logoPath());
        item.setUpdatedAt(Instant.now());
        tenants.save(tenant);
        OrganizationSettings saved = settings.save(item);
        audit.record(tenantId, actorId, "organization_settings", tenantId, "UPDATE", Map.of("name", saved.getOrganizationName()));
        return saved;
    }

    private String normalizeLocale(String locale) {
        String value = locale.toLowerCase(Locale.ROOT);
        if (!LOCALES.contains(value)) throw ApiException.badRequest("Locale must be fr, en or ar");
        return value;
    }

    public record CreateUser(String email, String fullName, String phone, String role, String locale) { }
    public record UpdateUser(String fullName, String phone, String locale, String role, Boolean active) { }
    public record UpdateProfile(String fullName, String phone, String locale, String theme) { }
    public record DepartmentRequest(String code, String name) { }
    public record StakeholderRequest(String name, String type) { }
    public record SettingsRequest(String organizationName, String defaultLocale, String logoPath) { }
}
