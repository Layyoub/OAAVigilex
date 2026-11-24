/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.security;

import java.util.UUID;

public record UserPrincipal(UUID id, UUID tenantId, String role, String email) { }
