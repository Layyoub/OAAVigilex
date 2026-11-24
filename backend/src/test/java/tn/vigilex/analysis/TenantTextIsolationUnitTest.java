/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.vigilex.audit.AuditService;
import tn.vigilex.common.ApiException;
import tn.vigilex.notification.NotificationService;
import tn.vigilex.repository.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TenantTextIsolationUnitTest {
    private TenantTextRepository tenantTexts;
    private TenantTextService service;

    @BeforeEach
    void setUp() {
        tenantTexts = mock(TenantTextRepository.class);
        service = new TenantTextService(tenantTexts, mock(LawTextRepository.class), mock(ActionRepository.class),
                mock(RequirementRepository.class), mock(ConformityHistoryRepository.class), mock(AppUserRepository.class),
                mock(AuditService.class), mock(NotificationService.class), mock(DepartmentRepository.class),
                mock(StakeholderRepository.class), mock(TextDepartmentRepository.class), mock(TextStakeholderRepository.class));
    }

    @Test
    void tenantCannotReadAnotherTenantsText() {
        UUID foreignTextId = UUID.randomUUID();
        UUID tenantA = UUID.randomUUID();
        when(tenantTexts.findByIdAndTenantId(foreignTextId, tenantA)).thenReturn(java.util.Optional.empty());

        assertThrows(ApiException.class, () -> service.get(tenantA, foreignTextId));
        verify(tenantTexts).findByIdAndTenantId(foreignTextId, tenantA);
        verify(tenantTexts, never()).findById(foreignTextId);
        verifyNoMoreInteractions(tenantTexts);
    }
}
