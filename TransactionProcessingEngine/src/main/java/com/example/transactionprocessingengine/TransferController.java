package com.example.transactionprocessingengine;

import com.fintech.grpc.ledger.BalanceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/api/v1/transfers", "/api/v1/transactions"})
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Endpoint Chuyển tiền (P2P Transfer).
     * Bắt buộc có header X-Idempotency-Key để chống lặp giao dịch (Idempotency Control).
     */
    @PostMapping({"", "/transfer"})
    public ResponseEntity<TransferResponseDto> transfer(
            @RequestHeader(value = "X-Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody TransferRequestDto request) {

        log.info("Received transfer request with idempotency key: {}", idempotencyKey);
        TransferResponseDto response = transferService.processTransfer(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint Nạp tiền (Topup) từ Cổng thanh toán hoặc Ngân hàng.
     */
    @PostMapping("/topup")
    public ResponseEntity<TransferResponseDto> topup(
            @RequestHeader(value = "X-Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody TopupRequestDto request) {

        log.info("Received topup request with idempotency key: {}", idempotencyKey);
        TransferResponseDto response = transferService.processTopup(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint truy vấn số dư ví tức thời qua gRPC sang Ledger Service.
     */
    @GetMapping("/balance/{accountId}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable("accountId") String accountId) {
        BalanceResponse balance = transferService.getBalance(accountId);
        return ResponseEntity.ok(Map.of(
                "accountId", balance.getAccountId(),
                "balance", balance.getBalance(),
                "currency", balance.getCurrency(),
                "lastEntryTimestamp", balance.getLastEntryTimestamp()
        ));
    }
}
