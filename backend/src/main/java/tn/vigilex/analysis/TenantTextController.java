/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.analysis;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.vigilex.common.ApiException;
import tn.vigilex.common.PageResponse;
import tn.vigilex.domain.*;
import tn.vigilex.repository.LawTextRepository;
import tn.vigilex.repository.TextDepartmentRepository;
import tn.vigilex.repository.TextStakeholderRepository;
import tn.vigilex.security.UserPrincipal;
import tn.vigilex.tenant.TenantContext;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/my-texts")
public class TenantTextController {
    private final TenantTextService service;
    private final LawTextRepository lawTexts;
    private final TextDepartmentRepository textDepartments;
    private final TextStakeholderRepository textStakeholders;
    public TenantTextController(TenantTextService service, LawTextRepository lawTexts,
                                TextDepartmentRepository textDepartments, TextStakeholderRepository textStakeholders) {
        this.service = service;
        this.lawTexts = lawTexts;
        this.textDepartments = textDepartments;
        this.textStakeholders = textStakeholders;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE')")
    public List<RegisterItem> add(@Valid @RequestBody AddTexts request, @AuthenticationPrincipal UserPrincipal user) {
        return service.addAll(TenantContext.requireTenantId(), user.id(), request.lawTextIds())
                .stream().map(this::map).toList();
    }

    @GetMapping
    public PageResponse<RegisterItem> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "25") int size) {
        if (page < 0 || size < 1 || size > 100) throw ApiException.badRequest("Invalid pagination bounds");
        return PageResponse.from(service.list(TenantContext.requireTenantId(), PageRequest.of(page, size))
                .map(this::map));
    }

    @GetMapping("/{id}")
    public RegisterItem get(@PathVariable UUID id) {
        return map(service.get(TenantContext.requireTenantId(), id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE')")
    public RegisterItem update(@PathVariable UUID id, @RequestBody TenantTextService.UpdateRequest request,
                               @AuthenticationPrincipal UserPrincipal user) {
        return map(service.update(TenantContext.requireTenantId(), user.id(), id, request));
    }

    @PatchMapping("/{id}/conformity")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE')")
    public RegisterItem conformity(@PathVariable UUID id, @Valid @RequestBody ConformityRequest request,
                                   @AuthenticationPrincipal UserPrincipal user) {
        return map(service.changeConformity(TenantContext.requireTenantId(), user.id(), user.role(), id,
                request.status(), request.note()));
    }

    @GetMapping("/{id}/history")
    public List<ConformityHistory> history(@PathVariable UUID id) {
        return service.history(TenantContext.requireTenantId(), id);
    }

    @GetMapping("/{id}/requirements")
    public List<Requirement> requirements(@PathVariable UUID id) {
        return service.requirements(TenantContext.requireTenantId(), id);
    }

    @PostMapping("/{id}/requirements")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','RESP_CONFORMITE','CONTRIBUTEUR')")
    public Requirement addRequirement(@PathVariable UUID id, @Valid @RequestBody RequirementRequest request,
                                      @AuthenticationPrincipal UserPrincipal user) {
        return service.addRequirement(TenantContext.requireTenantId(), user.id(), id, request.articleRef(), request.description());
    }

    private RegisterItem map(TenantText item) {
        LawText text = lawTexts.findById(item.getLawTextId())
                .orElseThrow(() -> ApiException.notFound("Library text not found"));
        return new RegisterItem(item.getId(), item.getLawTextId(), text.getReference(), text.getType(),
                text.getTitleFr(), text.getTitleAr(), item.getApplicability(), item.getApplicabilityReason(),
                item.getConformity(), item.getVerifiedOn(), item.getCurrentSituation(), item.getComment(),
                item.getOwnerId(), item.getNextReviewOn(), item.getUpdatedAt(),
                textDepartments.findAllByTenantTextId(item.getId()).stream().map(TextDepartment::getDepartmentId).toList(),
                textStakeholders.findAllByTenantTextId(item.getId()).stream().map(TextStakeholder::getStakeholderId).toList());
    }

    public record AddTexts(@NotEmpty @Size(max = 500) List<@NotNull UUID> lawTextIds) { }
    public record ConformityRequest(@NotNull @Size(max = 30) String status, @Size(max = 4000) String note) { }
    public record RequirementRequest(String articleRef, @NotNull @Size(min = 1, max = 5000) String description) { }
    public record RegisterItem(UUID id, UUID lawTextId, String reference, String type, String titleFr, String titleAr,
                               String applicability, String applicabilityReason, String conformity, LocalDate verifiedOn,
                               String currentSituation, String comment, UUID ownerId, LocalDate nextReviewOn,
                               java.time.Instant updatedAt, List<UUID> departmentIds, List<UUID> stakeholderIds) { }
}
