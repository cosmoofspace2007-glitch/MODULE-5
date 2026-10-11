package com.example.accountcoreledgerservices;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20, nullable = false)
    private AccountType accountType;
    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "VND";
    @Column(name = "balance", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getAccountNumber() {
        return this.id;
    }
    public void setAccountNumber(String accountNumber) {
        this.id = accountNumber;
    }
}
