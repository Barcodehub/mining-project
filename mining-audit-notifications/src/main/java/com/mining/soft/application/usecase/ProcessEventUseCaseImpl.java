package com.mining.soft.application.usecase;
import com.mining.soft.application.port.in.ProcessEventUseCase;
import com.mining.soft.application.port.out.AuditRepositoryPort;
import com.mining.soft.application.port.out.NotificationRepositoryPort;
import com.mining.soft.domain.model.AuditLog;
import com.mining.soft.domain.model.ExternalEvent;
import com.mining.soft.domain.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessEventUseCaseImpl implements ProcessEventUseCase {
    private final NotificationRepositoryPort notificationRepositoryPort;
    private final AuditRepositoryPort auditRepositoryPort;
    @Override
    public void process(ExternalEvent event) {
        log.info("Processing event: {}", event);
        // 1. Audit processing
        AuditLog auditLog = AuditLog.builder()
                .userId(event.getUserId())
                .action(event.getEventType())
                .entity("REPORT_OR_COMMENT") // Genérico, puede ajustarse por tipo de evento
                .entityId(event.getEntityId())
                .timestamp(event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now())
                .metadata(event.getMetadata())
                .build();
        auditRepositoryPort.save(auditLog);
        // 2. Notification processing
        if (event.getTargetUserId() != null) {
            String message = generateMessageFor(event);
            Notification notification = Notification.builder()
                    .userId(event.getTargetUserId())
                    .type(event.getEventType())
                    .message(message)
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepositoryPort.save(notification);
        }
    }
    private String generateMessageFor(ExternalEvent event) {
        return switch (event.getEventType()) {
            case "REPORT_CREATED" -> "A new report has been created requiring your attention.";
            case "REPORT_VALIDATED" -> "Your report has been validated.";
            case "REPORT_REJECTED" -> "Your report has been rejected.";
            case "COMMENT_CREATED" -> "A new comment was added to a post you are tracking.";
            default -> "System notification regarding an event.";
        };
    }
}
