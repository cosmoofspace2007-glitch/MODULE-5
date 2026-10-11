package com.example.transactionprocessingengine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponseDto {
    private boolean success;
    private String transactionId;
    private String status;
    private String idempotencyKey;
    private String message;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;
    private long timestamp;
    private boolean fromCache; // true nếu kết quả được lấy ngay từ Idempotency Cache của Redis
}
