package com.example.notificationauditservice;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class TransactionEvent {

    private String transactionId;
    private String accountId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String status;
    private Instant timestamp;


    public TransactionEvent(
            String transactionId,
            String accountId,
            String fromAccount,
            String toAccount,
            BigDecimal amount,
            String currency,
            String transactionType,
            String status,
            Instant timestamp
    ) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.transactionType = transactionType;
        this.status = status;
        this.timestamp = timestamp;
    }
}
