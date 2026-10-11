package com.example.transactionprocessingengine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String LOCK_PREFIX = "fintech:lock:account:";

    private final RedissonClient redissonClient;

    @Value("${fintech.idempotency.lock-wait-seconds:5}")
    private long waitTimeSeconds;

    @Value("${fintech.idempotency.lock-lease-seconds:10}")
    private long leaseTimeSeconds;

    public interface LockCallback<T> {
        T execute() throws Exception;
    }

    /**
     * Thực thi hành động với Distributed Lock được sắp xếp theo thứ tự (Lock Ordering).
     */
    public <T> T executeWithOrderedLock(String accountId1, String accountId2, LockCallback<T> callback) throws Exception {
        // 1. Sắp xếp thứ tự từ điển lexicographical để loại bỏ Deadlock: min trước, max sau
        String firstAccountId = accountId1.compareTo(accountId2) <= 0 ? accountId1 : accountId2;
        String secondAccountId = accountId1.compareTo(accountId2) <= 0 ? accountId2 : accountId1;

        RLock lock1 = redissonClient.getLock(LOCK_PREFIX + firstAccountId);
        RLock lock2 = redissonClient.getLock(LOCK_PREFIX + secondAccountId);

        boolean acquired1 = false;
        boolean acquired2 = false;

        try {
            log.debug("Acquiring first lock on: {}", firstAccountId);
            acquired1 = lock1.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS);
            if (!acquired1) {
                throw new IllegalStateException("Timeout acquiring lock for account: " + firstAccountId);
            }

            log.debug("Acquiring second lock on: {}", secondAccountId);
            acquired2 = lock2.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS);
            if (!acquired2) {
                throw new IllegalStateException("Timeout acquiring lock for account: " + secondAccountId);
            }

            log.debug("Both locks acquired safely ({}, {}). Executing business logic...",
                    firstAccountId, secondAccountId);
            return callback.execute();

        } finally {
            // Mở khóa theo thứ tự ngược lại
            if (acquired2 && lock2.isHeldByCurrentThread()) {
                lock2.unlock();
                log.debug("Released second lock on: {}", secondAccountId);
            }
            if (acquired1 && lock1.isHeldByCurrentThread()) {
                lock1.unlock();
                log.debug("Released first lock on: {}", firstAccountId);
            }
        }
    }
}
