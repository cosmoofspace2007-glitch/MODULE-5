package com.example.transactionprocessingengine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;


    @Value("${fintech.kafka.topics.transaction-events:fintech.transactions}")
    private String topicName;

    public void publishTransactionEvent(TransactionEvent event) {
        log.info("Publishing TransactionEvent to Kafka topic [{}]: txId={}, type={}",
                topicName, event.getTransactionId(), event.getEventType());

        // Dùng sourceAccountId làm Message Key để mọi giao dịch của cùng 1 tài khoản luôn đến cùng 1 partition
        kafkaTemplate.send(topicName, event.getSourceAccountId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish transaction event: txId={}", event.getTransactionId(), ex);
                    } else {
                        log.debug("Event published to partition {} offset {}",
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}

