/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.tenant;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();
    private TenantContext() { }
    public static void set(UUID tenantId) { CURRENT.set(tenantId); }
    public static Optional<UUID> current() { return Optional.ofNullable(CURRENT.get()); }
    public static UUID requireTenantId() {
        return current().orElseThrow(tn.vigilex.common.ApiException::forbidden);
    }
    public static void clear() { CURRENT.remove(); }
}
