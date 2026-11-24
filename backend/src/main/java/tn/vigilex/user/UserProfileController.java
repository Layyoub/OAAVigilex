/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.vigilex.domain.AppUser;
import tn.vigilex.organization.OrganizationService;
import tn.vigilex.organization.OrganizationController.UserView;
import tn.vigilex.security.UserPrincipal;
import tn.vigilex.tenant.TenantContext;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserProfileController {
    private final OrganizationService service;
    public UserProfileController(OrganizationService service) { this.service = service; }

    @GetMapping
    public UserView get(@AuthenticationPrincipal UserPrincipal user) {
        return UserView.from(service.profile(user.id(), TenantContext.requireTenantId()));
    }

    @PatchMapping
    public UserView update(@RequestBody OrganizationService.UpdateProfile request,
                           @AuthenticationPrincipal UserPrincipal user) {
        AppUser updated = service.updateProfile(user.id(), TenantContext.requireTenantId(), request);
        return UserView.from(updated);
    }
}
