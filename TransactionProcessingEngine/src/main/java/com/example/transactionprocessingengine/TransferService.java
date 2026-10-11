package com.example.transactionprocessingengine;

import com.fintech.grpc.ledger.BalanceResponse;
import com.fintech.grpc.ledger.LedgerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final IdempotencyService idempotencyService;
    private final DistributedLockService distributedLockService;
    private final LedgerGrpcClient ledgerGrpcClient;
    private final TransactionEventProducer transactionEventProducer;

    /**
     * Xử lý giao dịch Chuyển khoản (P2P Transfer) với kiểm soát Idempotency & Concurrency khắt khe.
     */
    public TransferResponseDto processTransfer(String idempotencyKey, TransferRequestDto request) {
        log.info("Processing transfer request: key={}, from={}, to={}, amount={}",
                idempotencyKey, request.getFromAccountId(), request.getToAccountId(), request.getAmount());

        // 1. KIỂM TRA IDEMPOTENCY TRÊN REDIS
        // Nếu cùng 1 Idempotency Key đã được xử lý xong trước đó -> Trả về ngay kết quả đã lưu trong cache
        Optional<TransferResponseDto> cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);
        if (cachedResponse.isPresent()) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            return cachedResponse.get();
        }

        // Đăng ký Idempotency Key trạng thái PROCESSING trên Redis (SETNX)
        boolean acquired = idempotencyService.tryAcquire(idempotencyKey);
        if (!acquired) {
            log.warn("Concurrent request with same idempotency key is already in progress: {}", idempotencyKey);
            // Trả mã lỗi 409 Conflict chuẩn mực của ngành tài chính
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Duplicate transaction request currently being processed (Idempotency Key: " + idempotencyKey + ")");
        }

        String transactionId = "TX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        try {
            // 2. THỰC THI KHÓA PHÂN TÁN CHỐNG DEADLOCK (LOCK ORDERING: min(accA, accB) trước)
            TransferResponseDto response = distributedLockService.executeWithOrderedLock(
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    () -> {
                        // 3. GỌI gRPC ĐỒNG BỘ SANG LEDGER SERVICE ĐỂ GHI SỔ CÁI KẾ TOÁN KÉP
                        LedgerResponse grpcResponse = ledgerGrpcClient.recordTransfer(
                                transactionId,
                                idempotencyKey,
                                request.getFromAccountId(),
                                request.getToAccountId(),
                                request.getAmount(),
                                request.getCurrency(),
                                request.getDescription()
                        );

                        TransferResponseDto res = TransferResponseDto.builder()
                                .success(grpcResponse.getSuccess())
                                .transactionId(grpcResponse.getTransactionId())
                                .status(grpcResponse.getStatus())
                                .idempotencyKey(idempotencyKey)
                                .message(grpcResponse.getMessage())
                                .fromBalanceAfter(BigDecimal.valueOf(grpcResponse.getFromBalanceAfter()))
                                .toBalanceAfter(BigDecimal.valueOf(grpcResponse.getToBalanceAfter()))
                                .timestamp(grpcResponse.getTimestamp())
                                .fromCache(false)
                                .build();

                        // 4. BẮN SỰ KIỆN GIAO DỊCH SANG APACHE KAFKA (ASYNC) CHO FRAUD & AUDIT SERVICES
                        TransactionEvent event = TransactionEvent.builder()
                                .transactionId(transactionId)
                                .idempotencyKey(idempotencyKey)
                                .eventType("TRANSFER")
                                .sourceAccountId(request.getFromAccountId())
                                .destinationAccountId(request.getToAccountId())
                                .amount(request.getAmount())
                                .currency(request.getCurrency())
                                .status("SUCCESS")
                                .fromBalanceAfter(res.getFromBalanceAfter())
                                .toBalanceAfter(res.getToBalanceAfter())
                                .timestamp(Instant.now())
                                .build();
                        transactionEventProducer.publishTransactionEvent(event);

                        return res;
                    }
            );

            // 5. LƯU KẾT QUẢ VÀO IDEMPOTENCY CACHE TRÊN REDIS (TTL 24H)
            idempotencyService.saveResponse(idempotencyKey, response);
            return response;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Transaction failed: txId={}, key={}", transactionId, idempotencyKey, e);
            // Xóa khóa Idempotency tạm nếu giao dịch thất bại do lỗi hệ thống để client có thể thử lại
            idempotencyService.releaseKey(idempotencyKey);
            throw new RuntimeException("Transaction processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Xử lý nạp tiền (Topup) từ Ngân hàng.
     */
    public TransferResponseDto processTopup(String idempotencyKey, TopupRequestDto request) {
        log.info("Processing topup request: key={}, userAccount={}, amount={}",
                idempotencyKey, request.getUserAccountId(), request.getAmount());

        Optional<TransferResponseDto> cachedResponse = idempotencyService.getCachedResponse(idempotencyKey);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        boolean acquired = idempotencyService.tryAcquire(idempotencyKey);
        if (!acquired) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate topup request in progress");
        }

        String transactionId = "TOPUP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        try {
            LedgerResponse grpcResponse = ledgerGrpcClient.recordTopup(
                    transactionId,
                    idempotencyKey,
                    request.getUserAccountId(),
                    request.getAmount(),
                    request.getCurrency(),
                    request.getReferenceCode()
            );

            TransferResponseDto response = TransferResponseDto.builder()
                    .success(grpcResponse.getSuccess())
                    .transactionId(grpcResponse.getTransactionId())
                    .status(grpcResponse.getStatus())
                    .idempotencyKey(idempotencyKey)
                    .message(grpcResponse.getMessage())
                    .toBalanceAfter(BigDecimal.valueOf(grpcResponse.getToBalanceAfter()))
                    .timestamp(grpcResponse.getTimestamp())
                    .fromCache(false)
                    .build();

            // Phát sự kiện nạp tiền lên Kafka
            TransactionEvent event = TransactionEvent.builder()
                    .transactionId(transactionId)
                    .idempotencyKey(idempotencyKey)
                    .eventType("TOPUP")
                    .sourceAccountId("ACC_SYSTEM_CLEARING")
                    .destinationAccountId(request.getUserAccountId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .status("SUCCESS")
                    .toBalanceAfter(response.getToBalanceAfter())
                    .timestamp(Instant.now())
                    .build();
            transactionEventProducer.publishTransactionEvent(event);

            idempotencyService.saveResponse(idempotencyKey, response);
            return response;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Topup failed: txId={}, key={}", transactionId, idempotencyKey, e);
            idempotencyService.releaseKey(idempotencyKey);
            throw new RuntimeException("Topup processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Truy vấn số dư tức thời từ Ledger Service qua gRPC.
     */
    public BalanceResponse getBalance(String accountId) {
        return ledgerGrpcClient.getAccountBalance(accountId);
    }
}

