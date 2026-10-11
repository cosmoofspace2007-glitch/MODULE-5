package com.example.notificationauditservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    @KafkaListener(
            topics = "fintech.transactions",
            groupId = "notification-audit-group"
    )
    public void consume(TransactionEvent event) {

        if (event == null) {
            return;
        }

        // Chỉ gửi thông báo đối với giao dịch thành công
        if (!"POSTED".equalsIgnoreCase(event.getStatus())) {
            return;
        }

        sendNotification(event);
    }

    private void sendNotification(TransactionEvent event) {

        System.out.println(
                "========== TRANSACTION NOTIFICATION =========="
        );

        System.out.println(
                "Transaction ID: "
                        + event.getTransactionId()
        );

        System.out.println(
                "Account: "
                        + event.getAccountId()
        );

        System.out.println(
                "Amount: "
                        + event.getAmount()
                        + " "
                        + event.getCurrency()
        );

        System.out.println(
                "Type: "
                        + event.getTransactionType()
        );

        System.out.println(
                "Message: Transaction completed successfully."
        );

        System.out.println(
                "==============================================="
        );
    }
}
