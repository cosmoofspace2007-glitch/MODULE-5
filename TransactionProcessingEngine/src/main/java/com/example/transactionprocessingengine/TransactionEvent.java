package com.example.transactionprocessingengine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String idempotencyKey;
    private String eventType; // 'TRANSFER', 'TOPUP', 'WITHDRAW'
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;
    private Instant timestamp;
}

