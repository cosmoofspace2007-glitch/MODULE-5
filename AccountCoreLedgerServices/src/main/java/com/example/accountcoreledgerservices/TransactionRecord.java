package com.example.accountcoreledgerservices;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_idempotency", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRecord {
    @Id
    @Column(name = "transaction_id", length = 64, nullable = false)
    private String transactionId;
    @Column(name = "idempotency_key", length = 128, nullable = false, unique = true)
    private String idempotencyKey;
    @Column(name = "tx_type", length = 30, nullable = false)
    private String txType; // TRANSFER, TOPUP, WITHDRAW, PAYMENT
    @Column(name = "source_account_id", length = 64, nullable = false)
    private String sourceAccountId;
    @Column(name = "destination_account_id", length = 64, nullable = false)
    private String destinationAccountId;
    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;
    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "VND";
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TransactionStatus status;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}

