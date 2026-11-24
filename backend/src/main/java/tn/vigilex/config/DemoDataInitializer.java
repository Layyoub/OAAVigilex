/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.domain.AppUser;
import tn.vigilex.repository.AppUserRepository;
import tn.vigilex.repository.TenantRepository;
import java.util.List;
import java.util.UUID;

@Component
@Profile("dev")
public class DemoDataInitializer implements ApplicationRunner {
    private final AppUserRepository users;
    private final TenantRepository tenants;
    private final PasswordEncoder passwords;
    private final String demoPassword;

    public DemoDataInitializer(AppUserRepository users, TenantRepository tenants, PasswordEncoder passwords,
                               @Value("${VIGILEX_DEMO_PASSWORD:ChangeMe-Vigilex-2026!}") String demoPassword) {
        this.users = users;
        this.tenants = tenants;
        this.passwords = passwords;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        create(null, "admin@vigilex.local", "Administrateur Vigilex", "SUPER_ADMIN");
        seedTenant("demo-nord", List.of(
                new DemoUser("admin@nord.vigilex.local", "Administrateur Nord", "TENANT_ADMIN"),
                new DemoUser("conformite@nord.vigilex.local", "Responsable conformité Nord", "RESP_CONFORMITE"),
                new DemoUser("contributeur@nord.vigilex.local", "Contributeur Nord", "CONTRIBUTEUR"),
                new DemoUser("lecture@nord.vigilex.local", "Lecteur Nord", "LECTEUR")));
        seedTenant("demo-sud", List.of(
                new DemoUser("admin@sud.vigilex.local", "Administrateur Sud", "TENANT_ADMIN"),
                new DemoUser("conformite@sud.vigilex.local", "Responsable conformité Sud", "RESP_CONFORMITE"),
                new DemoUser("contributeur@sud.vigilex.local", "Contributeur Sud", "CONTRIBUTEUR"),
                new DemoUser("lecture@sud.vigilex.local", "Lecteur Sud", "LECTEUR")));
    }

    private void seedTenant(String slug, List<DemoUser> tenantUsers) {
        UUID tenantId = tenants.findBySlug(slug).orElseThrow().getId();
        tenantUsers.forEach(user -> create(tenantId, user.email(), user.name(), user.role()));
    }

    private void create(UUID tenantId, String email, String name, String role) {
        if (users.existsByEmailIgnoreCase(email)) return;
        AppUser user = new AppUser();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setFullName(name);
        user.setRole(role);
        user.setPasswordHash(passwords.encode(demoPassword));
        users.save(user);
    }

    private record DemoUser(String email, String name, String role) { }
}
