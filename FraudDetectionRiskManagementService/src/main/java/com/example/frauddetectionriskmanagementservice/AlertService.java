package com.example.frauddetectionriskmanagementservice;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AlertService {

    private final StringRedisTemplate redisTemplate;

    public AlertService(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    public void createAlert(
            TransactionEvent event,
            String alertType,
            String description
    ) {

        System.out.println(
                "========== FRAUD ALERT =========="
        );

        System.out.println("Transaction ID: " + event.getTransactionId());

        System.out.println("Account ID: " + event.getAccountId());

        System.out.println("Alert Type: " + alertType);

        System.out.println("Description: " + description);

        System.out.println("=================================");
        // Khóa tài khoản tạm thời trong Redis
        temporarilyBlockAccount(event.getAccountId());
    }

    private void temporarilyBlockAccount(String accountId)
    {
        String key = "fraud:blocked:" + accountId;
        /*
         * Khóa tài khoản trong 15 phút.
         */
        redisTemplate.opsForValue()
                .set(key, "BLOCKED", Duration.ofMinutes(15));

        System.out.println("Account " + accountId + " has been temporarily blocked.");
    }

    public boolean isAccountBlocked(String accountId)
    {
        String key = "fraud:blocked:" + accountId;

        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}