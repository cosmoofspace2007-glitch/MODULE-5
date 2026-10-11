package com.example.transactionprocessingengine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "fintech:idempotency:";
    public static final String STATUS_PROCESSING = "PROCESSING";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fintech.idempotency.ttl-seconds:86400}")
    private long ttlSeconds;

    public boolean tryAcquire(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        // Đặt trạng thái ban đầu là PROCESSING với TTL 60 giây (đề phòng sự cố crash giữa chừng)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, STATUS_PROCESSING, Duration.ofSeconds(60));
        return Boolean.TRUE.equals(success);
    }

    /**
     * Kiểm tra xem Idempotency Key có đang trong quá trình thực thi (PROCESSING) hay không.
     */
    public boolean isProcessing(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        String val = redisTemplate.opsForValue().get(key);
        return STATUS_PROCESSING.equals(val);
    }

    public Optional<TransferResponseDto> getCachedResponse(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        String cachedJson = redisTemplate.opsForValue().get(key);

        if (cachedJson == null || STATUS_PROCESSING.equals(cachedJson)) {
            return Optional.empty();
        }

        try {
            TransferResponseDto response = objectMapper.readValue(cachedJson, TransferResponseDto.class);
            response.setFromCache(true);
            return Optional.of(response);
        } catch (Exception e) {
            log.error("Failed to parse cached response for key: {}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    /**
     * Lưu kết quả giao dịch thành công vào Redis với TTL 24 giờ.
     */
    public void saveResponse(String idempotencyKey, TransferResponseDto response) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.info("Cached idempotency response successfully for key: {}", idempotencyKey);
        } catch (Exception e) {
            log.error("Failed to serialize response for key: {}", idempotencyKey, e);
        }
    }

    /**
     * Xóa khóa nếu giao dịch bị lỗi hệ thống để cho phép client thử lại.
     */
    public void releaseKey(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        redisTemplate.delete(key);
    }
}
