package com.mining.soft.infrastructure.adapter.in.kafka;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.soft.application.port.in.ProcessEventUseCase;
import com.mining.soft.domain.model.ExternalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {
    private final ProcessEventUseCase processEventUseCase;
    private final ObjectMapper objectMapper;
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "false",
            dltTopicSuffix = "-dlt"
    )
    @KafkaListener(topics = "mining-events", groupId = "${spring.kafka.consumer.group-id:audit-notifications-group}")
    public void consumeEvent(String message) {
        try {
            log.info("Received event: {}", message);
            ExternalEvent event = objectMapper.readValue(message, ExternalEvent.class);
            processEventUseCase.process(event);
        } catch (Exception e) {
            log.error("Error processing event from Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process event", e);
        }
    }
}
