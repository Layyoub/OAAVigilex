/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.organization;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.vigilex.domain.*;
import tn.vigilex.security.UserPrincipal;
import tn.vigilex.tenant.TenantContext;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service = service; }
    private UUID tenant() { return TenantContext.requireTenantId(); }

    @GetMapping("/users")
    public List<UserView> users() { return service.users(tenant()).stream().map(UserView::from).toList(); }

    @PostMapping("/users")
    public UserView createUser(@Valid @RequestBody CreateUserRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        AppUser user = service.createUser(tenant(), actor.id(), new OrganizationService.CreateUser(
                request.email(), request.fullName(), request.phone(), request.role(), request.locale()));
        return UserView.from(user);
    }

    @PatchMapping("/users/{id}")
    public UserView updateUser(@PathVariable UUID id, @RequestBody OrganizationService.UpdateUser request,
                               @AuthenticationPrincipal UserPrincipal actor) {
        return UserView.from(service.updateUser(tenant(), actor.id(), id, request));
    }

    @GetMapping("/departments")
    public List<Department> departments() { return service.departments(tenant()); }
    @PostMapping("/departments")
    public Department createDepartment(@Valid @RequestBody DepartmentRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        return service.createDepartment(tenant(), actor.id(), new OrganizationService.DepartmentRequest(request.code(), request.name()));
    }
    @PutMapping("/departments/{id}")
    public Department updateDepartment(@PathVariable UUID id, @Valid @RequestBody DepartmentRequest request,
                                       @AuthenticationPrincipal UserPrincipal actor) {
        return service.updateDepartment(tenant(), actor.id(), id, new OrganizationService.DepartmentRequest(request.code(), request.name()));
    }
    @DeleteMapping("/departments/{id}")
    public void deleteDepartment(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal actor) {
        service.deleteDepartment(tenant(), actor.id(), id);
    }

    @GetMapping("/stakeholders")
    public List<Stakeholder> stakeholders() { return service.stakeholders(tenant()); }
    @PostMapping("/stakeholders")
    public Stakeholder createStakeholder(@Valid @RequestBody StakeholderRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        return service.createStakeholder(tenant(), actor.id(), new OrganizationService.StakeholderRequest(request.name(), request.type()));
    }
    @PutMapping("/stakeholders/{id}")
    public Stakeholder updateStakeholder(@PathVariable UUID id, @Valid @RequestBody StakeholderRequest request,
                                         @AuthenticationPrincipal UserPrincipal actor) {
        return service.updateStakeholder(tenant(), actor.id(), id, new OrganizationService.StakeholderRequest(request.name(), request.type()));
    }
    @DeleteMapping("/stakeholders/{id}")
    public void deleteStakeholder(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal actor) {
        service.deleteStakeholder(tenant(), actor.id(), id);
    }

    @GetMapping("/settings")
    public OrganizationSettings settings() { return service.settings(tenant()); }
    @PatchMapping("/settings")
    public OrganizationSettings updateSettings(@RequestBody OrganizationService.SettingsRequest request,
                                               @AuthenticationPrincipal UserPrincipal actor) {
        return service.updateSettings(tenant(), actor.id(), request);
    }

    public record CreateUserRequest(@Email @NotBlank String email, @NotBlank @Size(max = 180) String fullName,
                                    @Size(max = 40) String phone, @NotBlank String role, String locale) { }
    public record DepartmentRequest(@NotBlank @Size(max = 40) String code, @NotBlank @Size(max = 160) String name) { }
    public record StakeholderRequest(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 80) String type) { }
    public record UserView(UUID id, String email, String fullName, String phone, String role, String locale,
                           String theme, boolean active) {
        public static UserView from(AppUser user) {
            return new UserView(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                    user.getRole(), user.getLocale(), user.getTheme(), user.isActive());
        }
    }
}
