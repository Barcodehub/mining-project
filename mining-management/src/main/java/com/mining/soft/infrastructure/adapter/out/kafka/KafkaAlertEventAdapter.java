package com.mining.soft.infrastructure.adapter.out.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.soft.application.spi.IAlertEventPort;
import com.mining.soft.domain.model.EnvironmentalAlertEvent;
import com.mining.soft.infrastructure.configuration.AmbientalConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica alertas ambientales en el tópico dedicado (clave = clúster, para mantener
 * el orden por zona). El envío es asíncrono: la ingesta no espera al broker.
 * Semántica: al menos una vez, después de persistir la medición; el consumidor
 * deduplica por measurementId. (El patrón outbox transaccional queda como trabajo futuro.)
 */
@Slf4j
@Component
public class KafkaAlertEventAdapter implements IAlertEventPort {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final MeterRegistry registry;
    private final Counter failed;

    @Value("${mining.events.enabled:true}")
    private boolean eventsEnabled;

    public KafkaAlertEventAdapter(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                  AmbientalConfig cfg, MeterRegistry registry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = cfg.getAlertas().getTopico();
        this.registry = registry;
        this.failed = Counter.builder("environmental.alerts.publish.failed")
                .description("Alertas ambientales cuyo envío al broker falló").register(registry);
    }

    @Override
    public void publish(EnvironmentalAlertEvent event) {
        if (!eventsEnabled) return;
        try {
            String key = event.clusterId() != null ? event.clusterId() : event.deviceId();
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    failed.increment();
                    log.error("Fallo al publicar alerta {} en {}: {}", event.measurementId(), topic, ex.getMessage());
                } else {
                    Counter.builder("environmental.alerts.published")
                            .tag("level", event.alertLevel())
                            .tag("type", event.alertType())
                            .register(registry).increment();
                }
            });
        } catch (Exception e) {
            failed.increment();
            log.error("No se pudo serializar/enviar la alerta {}: {}", event.measurementId(), e.getMessage(), e);
        }
    }
}
