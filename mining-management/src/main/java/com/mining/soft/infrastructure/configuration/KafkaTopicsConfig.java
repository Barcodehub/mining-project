package com.mining.soft.infrastructure.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic environmentalAlertsTopic(AmbientalConfig cfg) {
        return TopicBuilder.name(cfg.getAlertas().getTopico()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic miningEventsTopic() {
        return TopicBuilder.name("mining-events").partitions(3).replicas(1).build();
    }
}
