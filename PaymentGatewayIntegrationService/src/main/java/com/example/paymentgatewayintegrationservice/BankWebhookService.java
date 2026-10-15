package com.example.paymentgatewayintegrationservice;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class BankWebhookService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    /*
     * Secret dùng để xác thực webhook từ ngân hàng.
     * Thực tế nên lấy từ environment variable / Secret Manager.
     */
    private final String bankWebhookSecret = "BANK_WEBHOOK_SECRET";
    /*
     * URL của Transaction Engine.
     */
    private final String transactionEngineUrl = "http://localhost:8082";

    public BankWebhookService(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = new RestTemplate();
    }

    public boolean processWebhook(
            BankWebhookPayload payload
    ) {

        // 1. Kiểm tra dữ liệu đầu vào
        if (payload == null ||
                payload.getBankRefId() == null ||
                payload.getAccountNumber() == null ||
                payload.getAmount() == null ||
                payload.getChecksum() == null) {

            throw new IllegalArgumentException(
                    "Invalid bank webhook payload"
            );
        }

        if (payload.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // 2. Kiểm tra checksum
        boolean validChecksum = verifyChecksum(payload);

        if (!validChecksum) {throw new SecurityException(
                "Invalid bank webhook checksum"
            );
        }

        // 3. Kiểm tra webhook đã xử lý chưa
        String redisKey = "bank:webhook:" + payload.getBankRefId();

        Boolean alreadyProcessed = redisTemplate.hasKey(redisKey);

        if (Boolean.TRUE.equals(alreadyProcessed)) {return false;}

        // 4. Đánh dấu bankRefId đã được xử lý
        Boolean locked = redisTemplate.opsForValue()
                        .setIfAbsent(
                                redisKey,
                                "PROCESSED",
                                24,
                                TimeUnit.HOURS
                        );

        /*
         * Nếu hai webhook giống nhau đến cùng lúc,
         * chỉ request nào SETNX thành công mới được xử lý.
         */
        if (!Boolean.TRUE.equals(locked)) {return false;}

        // 5. Gọi Transaction Engine để nạp tiền
        callTransactionEngine(payload);
        return true;
    }

    private boolean verifyChecksum(BankWebhookPayload payload)
    {
        String rawData =
                payload.getBankRefId()
                        + "|"
                        + payload.getAccountNumber()
                        + "|"
                        + payload.getAmount();

        String expectedChecksum =
                generateHmacSha256(
                        rawData,
                        bankWebhookSecret
                );

        return MessageDigest.isEqual(
                expectedChecksum.getBytes(StandardCharsets.UTF_8),
                payload.getChecksum()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    private String generateHmacSha256(
            String data,
            String secret
    ) {

        try {

            Mac mac =
                    Mac.getInstance(HMAC_ALGORITHM);

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            secret.getBytes(StandardCharsets.UTF_8),
                            HMAC_ALGORITHM
                    );

            mac.init(secretKey);

            byte[] hash =
                    mac.doFinal(
                            data.getBytes(StandardCharsets.UTF_8)
                    );

            return Base64.getEncoder()
                    .encodeToString(hash);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Cannot generate webhook checksum",
                    e
            );
        }
    }

    private void callTransactionEngine(
            BankWebhookPayload payload
    ) {

        String url = transactionEngineUrl + "/api/v1/transfers/topup";

        Map<String, Object> requestBody = Map.of(
                "userAccountId", payload.getAccountNumber(),
                "bankRefId", payload.getBankRefId(),
                "amount", payload.getAmount(),
                "currency", "VND"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Idempotency-Key", payload.getBankRefId());

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        ResponseEntity<String> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {

            throw new IllegalStateException(
                    "Transaction Engine rejected top-up"
            );
        }
    }
}

