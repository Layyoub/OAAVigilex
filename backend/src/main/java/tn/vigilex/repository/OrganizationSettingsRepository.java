/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.vigilex.domain.OrganizationSettings;
import java.util.UUID;

public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, UUID> { }
