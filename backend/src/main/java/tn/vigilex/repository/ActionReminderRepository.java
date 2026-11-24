/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.ActionReminder;
import java.time.LocalDate;
import java.util.UUID;

public interface ActionReminderRepository extends JpaRepository<ActionReminder, UUID> {
    boolean existsByActionIdAndReminderDateAndReminderType(UUID actionId, LocalDate reminderDate, String reminderType);
}
