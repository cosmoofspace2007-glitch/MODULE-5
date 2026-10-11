package com.example.transactionprocessingengine;

import com.fintech.grpc.ledger.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LedgerGrpcClient {

    private static final long GRPC_DEADLINE_SECONDS = 5;

    @GrpcClient("ledger-service")
    private LedgerServiceGrpc.LedgerServiceBlockingStub ledgerStub;

    public LedgerResponse recordTransfer(String transactionId, String idempotencyKey,
                                         String fromAccountId, String toAccountId,
                                         BigDecimal amount, String currency, String description) {

        log.info("Sending gRPC recordTransfer to ledger-service: txId={}", transactionId);

        TransferLedgerRequest request = TransferLedgerRequest.newBuilder()
                .setTransactionId(transactionId)
                .setIdempotencyKey(idempotencyKey)
                .setFromAccountId(fromAccountId)
                .setToAccountId(toAccountId)
                .setAmount(amount.doubleValue())
                .setCurrency(currency != null ? currency : "VND")
                .setDescription(description != null ? description : "")
                .build();

        try {
            // Thiết lập gRPC Deadline 5 giây để bảo vệ hệ thống không bị nghẽn luồng
            return ledgerStub.withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .recordTransfer(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error calling LedgerService.recordTransfer: code={}, desc={}",
                    e.getStatus().getCode(), e.getStatus().getDescription());
            throw new RuntimeException("LedgerService error: " + e.getStatus().getDescription(), e);
        }
    }

    public LedgerResponse recordTopup(String transactionId, String idempotencyKey,
                                      String userAccountId, BigDecimal amount,
                                      String currency, String referenceCode) {

        log.info("Sending gRPC recordTopup to ledger-service: txId={}", transactionId);

        TopupLedgerRequest request = TopupLedgerRequest.newBuilder()
                .setTransactionId(transactionId)
                .setIdempotencyKey(idempotencyKey)
                .setUserAccountId(userAccountId)
                .setAmount(amount.doubleValue())
                .setCurrency(currency != null ? currency : "VND")
                .setReferenceCode(referenceCode != null ? referenceCode : "")
                .build();

        try {
            return ledgerStub.withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .recordTopup(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error calling LedgerService.recordTopup: code={}, desc={}",
                    e.getStatus().getCode(), e.getStatus().getDescription());
            throw new RuntimeException("LedgerService error: " + e.getStatus().getDescription(), e);
        }
    }

    public BalanceResponse getAccountBalance(String accountId) {
        BalanceRequest request = BalanceRequest.newBuilder()
                .setAccountId(accountId)
                .build();

        try {
            return ledgerStub.withDeadlineAfter(GRPC_DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .getAccountBalance(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("LedgerService getBalance error: " + e.getStatus().getDescription(), e);
        }
    }
}

