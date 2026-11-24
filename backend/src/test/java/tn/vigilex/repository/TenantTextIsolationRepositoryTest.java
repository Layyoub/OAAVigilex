/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tn.vigilex.domain.LawText;
import tn.vigilex.domain.Tenant;
import tn.vigilex.domain.TenantText;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
class TenantTextIsolationRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vigilex_test").withUsername("vigilex").withPassword("vigilex");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TenantRepository tenants;
    @Autowired LawTextRepository lawTexts;
    @Autowired TenantTextRepository tenantTexts;

    @Test
    void tenantScopedLookupDoesNotExposeForeignRegisterEntries() {
        Tenant first = tenant("first");
        Tenant second = tenant("second");
        LawText law = new LawText();
        law.setReference("ISOLATION-TEST-" + UUID.randomUUID());
        law.setType("LAW");
        law.setTitleFr("Texte de test d’isolation");
        law.setStatus("IN_FORCE");
        law = lawTexts.saveAndFlush(law);

        TenantText registerItem = new TenantText();
        registerItem.setTenantId(first.getId());
        registerItem.setLawTextId(law.getId());
        registerItem = tenantTexts.saveAndFlush(registerItem);

        assertThat(tenantTexts.findByIdAndTenantId(registerItem.getId(), first.getId())).isPresent();
        assertThat(tenantTexts.findByIdAndTenantId(registerItem.getId(), second.getId())).isEmpty();
        assertThat(tenantTexts.findAllByTenantId(second.getId())).isEmpty();
    }

    private Tenant tenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName(slug);
        tenant.setSlug(slug + "-" + UUID.randomUUID());
        return tenants.saveAndFlush(tenant);
    }
}
