package com.mining.soft.infrastructure.adapter.out.kafka;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.soft.application.spi.IPublishEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisherAdapter implements IPublishEventPort {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "mining-events";

    @Value("${mining.events.enabled:true}")
    private boolean eventsEnabled;
    @Override
    public void publish(String eventType, Long entityId, Long userId, Long targetUserId, LocalDateTime timestamp, Map<String, Object> metadata) {
        if (!eventsEnabled) {
            return;
        }
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("entityId", entityId);
            event.put("userId", userId);
            event.put("targetUserId", targetUserId);
            event.put("timestamp", timestamp != null ? timestamp.toString() : LocalDateTime.now().toString());
            event.put("metadata", metadata != null ? metadata : new HashMap<>());
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, payload);
            if (log.isDebugEnabled()) {
                log.debug("Event published to {}: {}", TOPIC, payload);
            }
        } catch (Exception e) {
            log.error("Failed to publish event to Kafka topic {}: {}", TOPIC, e.getMessage(), e);
        }
    }
}
