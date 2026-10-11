package com.example.frauddetectionriskmanagementservice;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
public class FraudRuleEngine {

    private static final int MAX_TRANSACTIONS_PER_MINUTE = 5;

    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD =
            new BigDecimal("50000000");

    private final StringRedisTemplate redisTemplate;

    private final AlertService alertService;

    public FraudRuleEngine(
            StringRedisTemplate redisTemplate,
            AlertService alertService
    ) {
        this.redisTemplate = redisTemplate;
        this.alertService = alertService;
    }

    public void analyze(TransactionEvent event) {

        if (event.getAccountId() == null) {
            return;
        }

        boolean tooManyTransactions =
                checkTransactionFrequency(event);

        boolean largeTransaction =
                checkLargeTransaction(event);

        if (tooManyTransactions) {

            alertService.createAlert(
                    event,
                    "HIGH_FREQUENCY_TRANSACTION",
                    "More than 5 transactions within 1 minute"
            );
        }

        if (largeTransaction) {

            alertService.createAlert(
                    event,
                    "LARGE_TRANSACTION",
                    "Transaction amount exceeds risk threshold"
            );
        }
    }

    /**
     * Sliding Window trong Redis.
     *
     * Mỗi transaction được lưu vào Sorted Set.
     *
     * Score = timestamp milliseconds.
     */
    private boolean checkTransactionFrequency(
            TransactionEvent event
    ) {

        String key =
                "fraud:transactions:"
                        + event.getAccountId();

        long currentTime =
                System.currentTimeMillis();

        long oneMinuteAgo =
                currentTime - 60_000;

        // Xóa các transaction cũ hơn 1 phút
        redisTemplate.opsForZSet()
                .removeRangeByScore(
                        key,
                        0,
                        oneMinuteAgo
                );

        // Thêm transaction hiện tại
        redisTemplate.opsForZSet()
                .add(
                        key,
                        event.getTransactionId(),
                        currentTime
                );

        // Đếm số transaction trong 1 phút
        Long transactionCount =
                redisTemplate.opsForZSet()
                        .count(
                                key,
                                oneMinuteAgo,
                                currentTime
                        );

        // Đặt TTL để tránh Redis lưu key vô thời hạn
        redisTemplate.expire(
                key,
                Duration.ofMinutes(2)
        );

        return transactionCount != null
                && transactionCount > MAX_TRANSACTIONS_PER_MINUTE;
    }

    /**
     * Kiểm tra giao dịch có số tiền lớn bất thường.
     */
    private boolean checkLargeTransaction(
            TransactionEvent event
    ) {

        if (event.getAmount() == null) {
            return false;
        }

        return event.getAmount()
                .compareTo(
                        LARGE_TRANSACTION_THRESHOLD
                ) > 0;
    }
}

