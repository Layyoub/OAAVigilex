/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.domain.AppUser;
import tn.vigilex.domain.Notification;
import tn.vigilex.repository.AppUserRepository;
import tn.vigilex.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final AppUserRepository users;

    public NotificationService(NotificationRepository notifications, AppUserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Transactional
    public void notifyTenantAdmins(UUID tenantId, String type, Map<String, ?> payload) {
        for (AppUser user : users.findAllByTenantIdAndRoleIn(tenantId, List.of("TENANT_ADMIN", "RESP_CONFORMITE"))) {
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setType(type);
            notification.setPayload(new java.util.LinkedHashMap<>(payload));
            notifications.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(UUID userId, Pageable pageable) {
        return notifications.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void notifyUser(UUID userId, String type, Map<String, ?> payload) {
        Notification item = new Notification();
        item.setUserId(userId);
        item.setType(type);
        item.setPayload(new java.util.LinkedHashMap<>(payload));
        notifications.save(item);
    }

    @Transactional
    public Notification markRead(UUID userId, UUID notificationId) {
        Notification item = notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> tn.vigilex.common.ApiException.notFound("Notification not found"));
        if (item.getReadAt() == null) item.setReadAt(Instant.now());
        return notifications.save(item);
    }
}
