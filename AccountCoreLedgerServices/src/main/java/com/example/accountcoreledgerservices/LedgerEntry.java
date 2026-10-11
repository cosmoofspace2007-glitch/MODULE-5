package com.example.accountcoreledgerservices;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_account_id", columnList = "account_id"),
        @Index(name = "idx_ledger_tx_id", columnList = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {
    @Id
    @Column(name = "entry_id", length = 64, nullable = false)
    private String entryId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionRecord transaction;
    @Column(name = "account_id", length = 64, nullable = false)
    private String accountId;
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10, nullable = false)
    private EntryDirection direction; // DEBIT hoặc CREDIT
    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount; // Luôn là số dương tuyệt đối
    @Column(name = "balance_after", precision = 18, scale = 2, nullable = false)
    private BigDecimal balanceAfter; // balanceSnapshot: Số dư của tài khoản sau bút toán
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    // Helper getter tương thích balanceSnapshot
    public BigDecimal getBalanceSnapshot() {
        return this.balanceAfter;
    }
    public void setBalanceSnapshot(BigDecimal balanceSnapshot) {
        this.balanceAfter = balanceSnapshot;
    }
}