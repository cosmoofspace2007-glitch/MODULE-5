
package com.example.transactionprocessingengine;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, TransactionEvent> transactionEventProducerFactory(
            @Value("${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}")
            String bootstrapServers) {

        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalStateException(
                    "KAFKA_BOOTSTRAP_SERVERS is empty"
            );
        }

        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers.trim()
        );

        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JacksonJsonSerializer.class
        );

        return new DefaultKafkaProducerFactory<String, TransactionEvent>(
                config
        );
    }

    @Bean
    public KafkaTemplate<String, TransactionEvent> transactionEventKafkaTemplate(
            ProducerFactory<String, TransactionEvent> transactionEventProducerFactory) {

        return new KafkaTemplate<>(transactionEventProducerFactory);
    }
}

