package com.mining.soft.application.usecase;

import com.mining.soft.application.port.out.AuditRepositoryPort;
import com.mining.soft.application.port.out.NotificationRepositoryPort;
import com.mining.soft.domain.model.AuditLog;
import com.mining.soft.domain.model.EnvironmentalAlert;
import com.mining.soft.domain.model.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Procesamiento de alertas ambientales (consumidor)")
class ProcessEnvironmentalAlertUseCaseImplTest {

    private static EnvironmentalAlert alert(long id) {
        return new EnvironmentalAlert("ENVIRONMENTAL_ALERT", id, "LA_BAJA", "QLB-01", 7.37, -72.89,
                "CRITICO", "IMPACTO", "PUNTAJE", 0.8, 1.0, 1.0, 0.0,
                List.of("cn-agua-humano:CRITICO", "cn-agua-ecologico:CRITICO"),
                "2025-01-13T10:00", 1_000L, 1_005L);
    }

    @Test
    @DisplayName("Registra auditoría y notifica a cada destinatario configurado")
    void auditsAndNotifies() {
        AuditRepositoryPort audit = mock(AuditRepositoryPort.class);
        NotificationRepositoryPort notif = mock(NotificationRepositoryPort.class);
        when(audit.exists(any(), any(), any())).thenReturn(false);
        var uc = new ProcessEnvironmentalAlertUseCaseImpl(audit, notif, List.of(1L, 5L));

        assertTrue(uc.process(alert(99)));

        ArgumentCaptor<AuditLog> a = ArgumentCaptor.forClass(AuditLog.class);
        verify(audit).save(a.capture());
        assertEquals("ENVIRONMENTAL_DATA", a.getValue().getEntity());
        assertEquals(99L, a.getValue().getEntityId());
        assertNull(a.getValue().getUserId());
        ArgumentCaptor<Notification> n = ArgumentCaptor.forClass(Notification.class);
        verify(notif, times(2)).save(n.capture());
        assertEquals("ENVIRONMENTAL_ALERT_CRITICO", n.getValue().getType());
        assertTrue(n.getValue().getMessage().contains("cn-agua-humano:CRITICO"));
    }

    @Test
    @DisplayName("Entrega duplicada (al menos una vez) se descarta sin efectos")
    void duplicateIsIgnored() {
        AuditRepositoryPort audit = mock(AuditRepositoryPort.class);
        NotificationRepositoryPort notif = mock(NotificationRepositoryPort.class);
        when(audit.exists("ENVIRONMENTAL_DATA", 99L, "ENVIRONMENTAL_ALERT")).thenReturn(true);
        var uc = new ProcessEnvironmentalAlertUseCaseImpl(audit, notif, List.of(1L));

        assertFalse(uc.process(alert(99)));
        verify(audit, never()).save(any());
        verifyNoInteractions(notif);
    }
}
