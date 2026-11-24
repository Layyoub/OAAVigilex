/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.action;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.vigilex.common.ApiException;
import tn.vigilex.common.PageResponse;
import tn.vigilex.domain.ActionItem;
import tn.vigilex.security.UserPrincipal;
import tn.vigilex.tenant.TenantContext;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/actions")
public class ActionController {
    private final ActionService service;
    public ActionController(ActionService service) { this.service = service; }

    @GetMapping
    public PageResponse<ActionItem> list(@AuthenticationPrincipal UserPrincipal user,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "25") int size,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String priority,
                                         @RequestParam(required = false) UUID responsibleId,
                                         @RequestParam(required = false) java.time.LocalDate from,
                                         @RequestParam(required = false) java.time.LocalDate to) {
        if (page < 0 || size < 1 || size > 100) throw ApiException.badRequest("Invalid pagination bounds");
        if (from != null && to != null && from.isAfter(to)) throw ApiException.badRequest("Date range is invalid");
        return PageResponse.from(service.list(TenantContext.requireTenantId(), user.id(), user.role(), status, priority,
                responsibleId, from, to, PageRequest.of(page, size)));
    }

    @GetMapping("/kanban")
    public java.util.Map<String, java.util.List<ActionItem>> kanban(@AuthenticationPrincipal UserPrincipal user) {
        var all = service.list(TenantContext.requireTenantId(), user.id(), user.role(), null, null,
                null, null, null, PageRequest.of(0, 1000)).getContent();
        return java.util.Map.of("TO_DO", all.stream().filter(a -> "TO_DO".equals(a.getStatus())).toList(),
                "IN_PROGRESS", all.stream().filter(a -> "IN_PROGRESS".equals(a.getStatus())).toList(),
                "DONE", all.stream().filter(a -> "DONE".equals(a.getStatus())).toList());
    }

    @GetMapping("/calendar")
    public java.util.List<ActionItem> calendar(@AuthenticationPrincipal UserPrincipal user,
                                                @RequestParam java.time.LocalDate from,
                                                @RequestParam java.time.LocalDate to) {
        if (from.isAfter(to) || to.isAfter(from.plusYears(2))) throw ApiException.badRequest("Calendar range is invalid");
        return service.list(TenantContext.requireTenantId(), user.id(), user.role(), null, null,
                null, from, to, PageRequest.of(0, 500)).getContent();
    }

    @GetMapping("/summary")
    public ActionService.ActionSummary summary() {
        return service.summary(TenantContext.requireTenantId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE')")
    public ActionItem create(@Valid @RequestBody CreateAction request, @AuthenticationPrincipal UserPrincipal user) {
        return service.create(TenantContext.requireTenantId(), user.id(), new ActionService.CreateRequest(
                request.tenantTextId(), request.title(), request.description(), request.responsibleId(),
                request.responsiblePhone(), request.priority(), request.dueOn()));
    }

    @GetMapping("/{id}")
    public ActionItem get(@PathVariable UUID id) {
        return service.get(TenantContext.requireTenantId(), id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE','CONTRIBUTEUR')")
    public ActionItem update(@PathVariable UUID id, @RequestBody ActionService.UpdateRequest request,
                             @AuthenticationPrincipal UserPrincipal user) {
        return service.update(TenantContext.requireTenantId(), user.id(), user.role(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE')")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        service.delete(TenantContext.requireTenantId(), user.id(), id);
    }

    public record CreateAction(@NotNull UUID tenantTextId, @NotBlank @Size(max = 300) String title,
                               @Size(max = 10000) String description, UUID responsibleId,
                               @Size(max = 40) String responsiblePhone, String priority,
                               java.time.LocalDate dueOn) { }
}
