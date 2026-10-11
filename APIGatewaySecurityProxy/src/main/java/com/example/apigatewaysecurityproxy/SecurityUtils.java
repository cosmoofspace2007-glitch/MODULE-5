package com.example.apigatewaysecurityproxy;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class SecurityUtils {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public String generateSignature(
            String method,
            String path,
            String timestamp,
            String body,
            String secretKey
    ) {
        try {
            String payload =
                    method + "|" +
                            path + "|" +
                            timestamp + "|" +
                            body;

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);

            SecretKeySpec secretKeySpec =
                    new SecretKeySpec(
                            secretKey.getBytes(StandardCharsets.UTF_8),
                            HMAC_ALGORITHM
                    );

            mac.init(secretKeySpec);

            byte[] hash =
                    mac.doFinal(
                            payload.getBytes(StandardCharsets.UTF_8)
                    );

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to generate HMAC-SHA256 signature",
                    e
            );
        }
    }

    public boolean isValidSignature(
            String expectedSignature,
            String actualSignature
    ) {
        if (expectedSignature == null ||
                actualSignature == null) {
            return false;
        }

        byte[] expected =
                expectedSignature.getBytes(StandardCharsets.UTF_8);

        byte[] actual =
                actualSignature.getBytes(StandardCharsets.UTF_8);

        // Chống Timing Attack
        return MessageDigest.isEqual(expected, actual);
    }
}

