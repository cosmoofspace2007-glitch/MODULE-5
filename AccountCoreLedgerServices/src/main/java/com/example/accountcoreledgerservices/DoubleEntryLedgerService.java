package com.example.accountcoreledgerservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoubleEntryLedgerService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    @Value("${fintech.ledger.system-clearing-account-id:ACC_SYSTEM_CLEARING}")
    private String systemClearingAccountId;
    /**
     * Thực hiện bút toán Kế toán Kép cho giao dịch Chuyển khoản (P2P Transfer).
     * Mức cô lập giao dịch: REPEATABLE_READ để chống đọc rác và Non-repeatable read.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransferResult recordTransfer(String transactionId, String idempotencyKey,
                                         String fromAccountId, String toAccountId,
                                         BigDecimal amount, String currency, String description) {
        log.info("Processing ledger transfer: txId={}, from={}, to={}, amount={}",
                transactionId, fromAccountId, toAccountId, amount);
        // 1. Kiểm tra Idempotency tại tầng Database Sổ cái
        Optional<TransactionRecord> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent()) {
            TransactionRecord tx = existingTx.get();
            log.info("Transaction already processed in Ledger with idempotencyKey={}", idempotencyKey);
            Account fromAcc = accountRepository.findById(tx.getSourceAccountId()).orElseThrow();
            Account toAcc = accountRepository.findById(tx.getDestinationAccountId()).orElseThrow();
            return new TransferResult(true, tx.getTransactionId(), tx.getStatus().name(),
                    "Transaction previously processed", fromAcc.getBalance(), toAcc.getBalance(),
                    tx.getCreatedAt().toEpochMilli());
        }
        // 2. Validate dữ liệu đầu vào
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly positive");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        // 3. Khóa tài khoản theo thứ tự ID ASC (Lock Ordering chống Deadlock tại Database)
        List<String> accountIds = Arrays.asList(fromAccountId, toAccountId);
        Collections.sort(accountIds);
        List<Account> lockedAccounts = accountRepository.findAccountsForUpdateOrdered(accountIds);
        Account fromAccount = lockedAccounts.stream()
                .filter(a -> a.getId().equals(fromAccountId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Source account not found: " + fromAccountId));
        Account toAccount = lockedAccounts.stream()
                .filter(a -> a.getId().equals(toAccountId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Destination account not found: " + toAccountId));
        // 4. Kiểm tra số dư người gửi
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient funds for account {}. Available: {}, Required: {}",
                    fromAccountId, fromAccount.getBalance(), amount);
            throw new IllegalStateException("Insufficient funds in account: " + fromAccountId);
        }
        // 5. Cập nhật số dư Snapshot (Chỉ cập nhật cùng lúc với việc ghi 2 dòng Ledger Entry)
        BigDecimal fromBalanceAfter = fromAccount.getBalance().subtract(amount);
        BigDecimal toBalanceAfter = toAccount.getBalance().add(amount);
        fromAccount.setBalance(fromBalanceAfter);
        toAccount.setBalance(toBalanceAfter);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        // 6. Lưu phiếu giao dịch gốc (Transaction)
        TransactionRecord txRecord = TransactionRecord.builder()
                .transactionId(transactionId)
                .idempotencyKey(idempotencyKey)
                .txType("TRANSFER")
                .sourceAccountId(fromAccountId)
                .destinationAccountId(toAccountId)
                .amount(amount)
                .currency(currency != null ? currency : "VND")
                .status(TransactionStatus.POSTED)
                .description(description)
                .createdAt(Instant.now())
                .build();
        transactionRepository.save(txRecord);
        // 7. Ghi 2 dòng Sổ cái Kế toán Kép (Double-Entry Ledger) - BẤT BIẾN
        // Dòng 1: Ghi NỢ (DEBIT) cho người chuyển (-amount)
        LedgerEntry debitEntry = LedgerEntry.builder()
                .entryId(UUID.randomUUID().toString())
                .transaction(txRecord)
                .accountId(fromAccountId)
                .direction(EntryDirection.DEBIT)
                .amount(amount)
                .balanceAfter(fromBalanceAfter)
                .createdAt(Instant.now())
                .build();
        // Dòng 2: Ghi CÓ (CREDIT) cho người nhận (+amount)
        LedgerEntry creditEntry = LedgerEntry.builder()
                .entryId(UUID.randomUUID().toString())
                .transaction(txRecord)
                .accountId(toAccountId)
                .direction(EntryDirection.CREDIT)
                .amount(amount)
                .balanceAfter(toBalanceAfter)
                .createdAt(Instant.now())
                .build();
        // 8. KIỂM TRA QUY TẮC BẤT BIẾN TÀI CHÍNH: Debit == Credit (Zero-sum Invariant)
        if (debitEntry.getAmount().compareTo(creditEntry.getAmount()) != 0) {
            throw new IllegalStateException("CRITICAL: Financial Double-Entry violation! Debit does not equal Credit");
        }
        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);
        log.info("Ledger entry committed successfully: txId={}, Debit={}, Credit={}",
                transactionId, debitEntry.getEntryId(), creditEntry.getEntryId());
        return new TransferResult(true, transactionId, TransactionStatus.POSTED.name(),
                "Transfer processed successfully", fromBalanceAfter, toBalanceAfter, txRecord.getCreatedAt().toEpochMilli());
    }
    /**
     * Bút toán nạp tiền từ Ngân hàng (Topup).
     * Nợ (DEBIT) tài khoản trung gian hệ thống (ACC_SYSTEM_CLEARING).
     * Có (CREDIT) tài khoản ví người dùng.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransferResult recordTopup(String transactionId, String idempotencyKey,
                                      String userAccountId, BigDecimal amount,
                                      String currency, String referenceCode) {
        log.info("Processing ledger topup: txId={}, userAccount={}, amount={}", transactionId, userAccountId, amount);
        // Tái sử dụng logic kế toán kép: Chuyển từ System Clearing sang ví người dùng
        return recordTransfer(transactionId, idempotencyKey, systemClearingAccountId, userAccountId,
                amount, currency, "Bank Topup: " + referenceCode);
    }
    @Transactional(readOnly = true)
    public Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
    }
    public record TransferResult(
            boolean success,
            String transactionId,
            String status,
            String message,
            BigDecimal fromBalanceAfter,
            BigDecimal toBalanceAfter,
            long timestamp
    ) {}
}
