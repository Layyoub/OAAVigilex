/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.notification;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.vigilex.common.ApiException;
import tn.vigilex.common.PageResponse;
import tn.vigilex.domain.Notification;
import tn.vigilex.security.UserPrincipal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping
    public PageResponse<Notification> list(@AuthenticationPrincipal UserPrincipal user,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "25") int size) {
        if (page < 0 || size < 1 || size > 100) throw ApiException.badRequest("Invalid pagination bounds");
        return PageResponse.from(service.list(user.id(), PageRequest.of(page, size)));
    }
    @PatchMapping("/{id}/read")
    public Notification markRead(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal user) {
        return service.markRead(user.id(), id);
    }
}
