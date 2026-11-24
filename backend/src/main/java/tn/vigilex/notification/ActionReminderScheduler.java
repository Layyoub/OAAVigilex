/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.notification;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.domain.ActionItem;
import tn.vigilex.domain.ActionReminder;
import tn.vigilex.repository.ActionReminderRepository;
import tn.vigilex.repository.ActionRepository;
import tn.vigilex.repository.AppUserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Component
public class ActionReminderScheduler {
    private final ActionRepository actions;
    private final ActionReminderRepository reminders;
    private final AppUserRepository users;
    private final NotificationService notifications;
    private final JavaMailSender mail;

    public ActionReminderScheduler(ActionRepository actions, ActionReminderRepository reminders,
                                   AppUserRepository users, NotificationService notifications, JavaMailSender mail) {
        this.actions = actions;
        this.reminders = reminders;
        this.users = users;
        this.notifications = notifications;
        this.mail = mail;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Africa/Tunis")
    @Transactional
    public void sendDailyReminders() {
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Tunis"));
        actions.findAllByDueOnAndStatusNot(today.plusDays(7), "DONE").forEach(action -> send(action, today, "DUE_IN_7_DAYS"));
        actions.findAllByDueOnAndStatusNot(today.plusDays(1), "DONE").forEach(action -> send(action, today, "DUE_TOMORROW"));
        actions.findAllByDueOnBeforeAndStatusNot(today, "DONE").forEach(action -> send(action, today, "OVERDUE"));
    }

    private void send(ActionItem action, LocalDate today, String type) {
        if (action.getResponsibleId() == null
                || reminders.existsByActionIdAndReminderDateAndReminderType(action.getId(), today, type)) return;
        var recipient = users.findByIdAndTenantId(action.getResponsibleId(), action.getTenantId()).orElse(null);
        if (recipient == null || !recipient.isActive()) return;
        ActionReminder reminder = new ActionReminder();
        reminder.setActionId(action.getId());
        reminder.setReminderDate(today);
        reminder.setReminderType(type);
        reminders.save(reminder);
        notifications.notifyUser(recipient.getId(), type, Map.of(
                "actionId", action.getId(), "title", action.getTitle(), "dueOn", action.getDueOn()));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipient.getEmail());
        message.setSubject("Rappel Vigilex : " + action.getTitle());
        message.setText("L'action « " + action.getTitle() + " » arrive à échéance le " + action.getDueOn() + ".");
        mail.send(message);
    }
}
