package com.example.frauddetectionriskmanagementservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    private final FraudRuleEngine fraudRuleEngine;

    public TransactionEventConsumer(
            FraudRuleEngine fraudRuleEngine
    ) {
        this.fraudRuleEngine = fraudRuleEngine;
    }

    @KafkaListener(topics = "fintech.transactions", groupId = "fraud-detection-group")
    public void consume(TransactionEvent event) {
        if (event == null) {return;}

        System.out.println(
                "Received transaction event: "
                        + event.getTransactionId()
        );
        fraudRuleEngine.analyze(event);
    }
}
