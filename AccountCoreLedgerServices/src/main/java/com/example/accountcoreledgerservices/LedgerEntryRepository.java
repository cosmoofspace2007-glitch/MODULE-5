package com.example.accountcoreledgerservices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, String> {
    List<LedgerEntry> findByTransactionTransactionId(String transactionId);
    List<LedgerEntry> findByAccountIdOrderByCreatedAtDesc(String accountId);
    /**
     * Tính toán số dư thực tế từ toàn bộ lịch sử sổ cái (Audit Balance).
     * Số dư tài khoản = SUM(CREDIT) - SUM(DEBIT).
     */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END), 0.00)
        FROM LedgerEntry e
        WHERE e.accountId = :accountId
    """)
    BigDecimal calculateBalanceFromLedger(@Param("accountId") String accountId);
    /**
     * Hàm đối soát toàn hệ thống: sumDebitAndCredit().
     * Kiểm tra chênh lệch: Tổng Credit trừ Tổng Debit trên toàn bộ Sổ cái.
     * Quy tắc Kế toán kép: Giá trị này BẮT BUỘC PHẢI BẰNG 0.00 (Zero Financial Error).
     */
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END), 0.00)
        FROM LedgerEntry e
    """)
    BigDecimal sumDebitAndCredit();
}