package com.example.accountcoreledgerservices;

import com.fintech.grpc.ledger.*;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class LedgerGrpcServiceImpl extends LedgerServiceGrpc.LedgerServiceImplBase {
    private final DoubleEntryLedgerService doubleEntryLedgerService;

    public void recordTransfer(com.fintech.grpc.ledger.TransferLedgerRequest request, StreamObserver<LedgerResponse> responseObserver) {
        log.info("Received gRPC recordTransfer: txId={}, from={}, to={}, amount={}",
                request.getTransactionId(), request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        try {
            DoubleEntryLedgerService.TransferResult result = doubleEntryLedgerService.recordTransfer(
                    request.getTransactionId(),
                    request.getIdempotencyKey(),
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    BigDecimal.valueOf(request.getAmount()),
                    request.getCurrency(),
                    request.getDescription()
            );
            LedgerResponse response = LedgerResponse.newBuilder()
                    .setSuccess(result.success())
                    .setTransactionId(result.transactionId())
                    .setStatus(result.status())
                    .setMessage(result.message())
                    .setFromBalanceAfter(result.fromBalanceAfter().doubleValue())
                    .setToBalanceAfter(result.toBalanceAfter().doubleValue())
                    .setTimestamp(result.timestamp())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            log.warn("gRPC recordTransfer invalid argument: {}", e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalStateException e) {
            log.warn("gRPC recordTransfer business error: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        } catch (NoSuchElementException e) {
            log.warn("gRPC recordTransfer account not found: {}", e.getMessage());
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC recordTransfer internal error", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Internal ledger error: " + e.getMessage()).asRuntimeException());
        }
    }


    public void recordTopup(TopupLedgerRequest request, StreamObserver<LedgerResponse> responseObserver) {
        log.info("Received gRPC recordTopup: txId={}, userAccount={}, amount={}",
                request.getTransactionId(), request.getUserAccountId(), request.getAmount());
        try {
            DoubleEntryLedgerService.TransferResult result = doubleEntryLedgerService.recordTopup(
                    request.getTransactionId(),
                    request.getIdempotencyKey(),
                    request.getUserAccountId(),
                    BigDecimal.valueOf(request.getAmount()),
                    request.getCurrency(),
                    request.getReferenceCode()
            );
            LedgerResponse response = LedgerResponse.newBuilder()
                    .setSuccess(result.success())
                    .setTransactionId(result.transactionId())
                    .setStatus(result.status())
                    .setMessage(result.message())
                    .setFromBalanceAfter(result.fromBalanceAfter().doubleValue())
                    .setToBalanceAfter(result.toBalanceAfter().doubleValue())
                    .setTimestamp(result.timestamp())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC recordTopup error", e);
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }


    public void getAccountBalance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        try {
            Account account = doubleEntryLedgerService.getAccount(request.getAccountId());
            BalanceResponse response = BalanceResponse.newBuilder()
                    .setAccountId(account.getId())
                    .setBalance(account.getBalance().doubleValue())
                    .setCurrency(account.getCurrency())
                    .setLastEntryTimestamp(account.getUpdatedAt() != null ? account.getUpdatedAt().toEpochMilli() : 0)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NoSuchElementException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Account not found: " + request.getAccountId()).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error retrieving balance: " + e.getMessage()).asRuntimeException());
        }
    }
}
