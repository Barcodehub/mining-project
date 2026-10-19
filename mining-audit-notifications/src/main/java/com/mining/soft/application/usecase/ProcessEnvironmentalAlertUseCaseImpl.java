package com.mining.soft.application.usecase;

import com.mining.soft.application.port.in.ProcessEnvironmentalAlertUseCase;
import com.mining.soft.application.port.out.AuditRepositoryPort;
import com.mining.soft.application.port.out.NotificationRepositoryPort;
import com.mining.soft.domain.model.AuditLog;
import com.mining.soft.domain.model.EnvironmentalAlert;
import com.mining.soft.domain.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProcessEnvironmentalAlertUseCaseImpl implements ProcessEnvironmentalAlertUseCase {

    static final String ENTITY = "ENVIRONMENTAL_DATA";
    static final String ACTION = "ENVIRONMENTAL_ALERT";

    private final AuditRepositoryPort auditRepositoryPort;
    private final NotificationRepositoryPort notificationRepositoryPort;
    private final List<Long> recipients;

    public ProcessEnvironmentalAlertUseCaseImpl(AuditRepositoryPort auditRepositoryPort,
                                                NotificationRepositoryPort notificationRepositoryPort,
                                                @Value("${alertas.destinatarios:1}") List<Long> recipients) {
        this.auditRepositoryPort = auditRepositoryPort;
        this.notificationRepositoryPort = notificationRepositoryPort;
        this.recipients = recipients;
    }

    @Override
    public boolean process(EnvironmentalAlert alert) {
        // Idempotencia: la entrega es "al menos una vez"; se descarta un duplicado por measurementId.
        if (auditRepositoryPort.exists(ENTITY, alert.measurementId(), ACTION)) {
            log.debug("Alerta duplicada descartada: {}", alert.measurementId());
            return false;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("clusterId", alert.clusterId());
        metadata.put("deviceId", alert.deviceId());
        metadata.put("alertLevel", alert.alertLevel());
        metadata.put("alertType", alert.alertType());
        metadata.put("levelBasis", alert.levelBasis());
        metadata.put("totalRiskScore", alert.totalRiskScore());
        metadata.put("exceedances", alert.exceedances());
        metadata.put("measurementTimestamp", alert.measurementTimestamp());
        metadata.put("ingestedAtEpochMs", alert.ingestedAtEpochMs());

        auditRepositoryPort.save(AuditLog.builder()
                .userId(null)
                .action(ACTION)
                .entity(ENTITY)
                .entityId(alert.measurementId())
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .metadata(metadata)
                .build());

        String message = String.format("Alerta %s (%s) en el clúster %s, dispositivo %s. Superaciones: %s",
                alert.alertLevel(), alert.alertType(), alert.clusterId(), alert.deviceId(),
                alert.exceedances() == null || alert.exceedances().isEmpty() ? "ninguna"
                        : String.join(", ", alert.exceedances()));
        for (Long recipient : recipients) {
            notificationRepositoryPort.save(Notification.builder()
                    .userId(recipient)
                    .type(ACTION + "_" + alert.alertLevel())
                    .message(message)
                    .read(false)
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build());
        }
        return true;
    }
}
