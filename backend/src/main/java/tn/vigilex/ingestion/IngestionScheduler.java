/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.ingestion;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.admin.AdminService;
import tn.vigilex.domain.AppUser;
import tn.vigilex.repository.AppUserRepository;
import java.util.List;
import java.util.UUID;

@Component
public class IngestionScheduler {
    private final List<SourceAdapter> adapters;
    private final AppUserRepository users;
    private final AdminService admin;
    public IngestionScheduler(List<SourceAdapter> adapters, AppUserRepository users, AdminService admin) {
        this.adapters = adapters;
        this.users = users;
        this.admin = admin;
    }

    @Scheduled(cron = "0 0 3 * * MON", zone = "Africa/Tunis")
    public void ingestAuthorizedSourcesWeekly() {
        AppUser actor = users.findFirstByRoleAndTenantIdIsNull("SUPER_ADMIN").orElse(null);
        if (actor == null) return;
        runNow(actor.getId());
    }

    public int runNow(UUID actorId) {
        int total = 0;
        for (SourceAdapter adapter : adapters) {
            if (adapter.enabled()) {
                List<ImportedLawText> imported = adapter.fetch();
                if (!imported.isEmpty()) total += admin.ingestTexts(actorId,
                        imported.stream().map(AdminService.TextInput::from).toList());
            }
        }
        return total;
    }

    @Transactional(readOnly = true)
    public List<String> availableAdapters() {
        return adapters.stream().map(adapter -> adapter.name() + (adapter.enabled() ? ":enabled" : ":disabled")).toList();
    }
}
