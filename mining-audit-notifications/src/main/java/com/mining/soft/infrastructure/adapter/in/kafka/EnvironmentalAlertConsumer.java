package com.mining.soft.infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mining.soft.application.port.in.ProcessEnvironmentalAlertUseCase;
import com.mining.soft.domain.model.EnvironmentalAlert;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Consume alertas ambientales y mide:
 *  - alert.e2e.latency    : ingesta en mining-management → notificación persistida (punta a punta)
 *  - alert.broker.latency : publicación en Kafka → notificación persistida
 * Ambos servicios corren sobre el mismo reloj del host (contenedores Docker), así que
 * la diferencia de marcas de tiempo no tiene desfase de reloj entre máquinas.
 */
@Slf4j
@Component
public class EnvironmentalAlertConsumer {

    private final ProcessEnvironmentalAlertUseCase useCase;
    private final ObjectMapper objectMapper;
    private final Timer e2eLatency;
    private final Timer brokerLatency;
    private final MeterRegistry registry;
    private final Counter duplicates;

    public EnvironmentalAlertConsumer(ProcessEnvironmentalAlertUseCase useCase, ObjectMapper objectMapper,
                                      MeterRegistry registry) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.e2eLatency = latencyTimer("alert.e2e.latency",
                "Latencia de punta a punta: ingesta de la medición → notificación persistida", registry);
        this.brokerLatency = latencyTimer("alert.broker.latency",
                "Latencia desde la publicación en Kafka → notificación persistida", registry);
        this.duplicates = Counter.builder("alert.duplicates.discarded")
                .description("Entregas duplicadas descartadas por idempotencia").register(registry);
    }

    private static Timer latencyTimer(String name, String description, MeterRegistry registry) {
        return Timer.builder(name)
                .description(description)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(30))
                .register(registry);
    }

    @KafkaListener(topics = "${alertas.topico:environmental-alerts}",
            groupId = "${alertas.grupo:audit-notifications-alerts}",
            concurrency = "${alertas.concurrencia:3}")
    public void consume(String message) {
        EnvironmentalAlert alert;
        try {
            alert = objectMapper.readValue(message, EnvironmentalAlert.class);
        } catch (Exception e) {
            log.error("Alerta ambiental ilegible, se descarta: {}", e.getMessage());
            Counter.builder("alert.malformed").register(registry).increment();
            return;
        }
        boolean processed = useCase.process(alert);
        if (!processed) {
            duplicates.increment();
            return;
        }
        long now = System.currentTimeMillis();
        if (alert.ingestedAtEpochMs() > 0) e2eLatency.record(Duration.ofMillis(now - alert.ingestedAtEpochMs()));
        if (alert.publishedAtEpochMs() > 0) brokerLatency.record(Duration.ofMillis(now - alert.publishedAtEpochMs()));
        Counter.builder("alert.consumed")
                .tag("level", String.valueOf(alert.alertLevel()))
                .register(registry).increment();
    }
}
