package com.example.notificationauditservice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class FinancialReconciliationService {

    private final JdbcTemplate jdbcTemplate;

    public FinancialReconciliationService(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Thực hiện đối soát toàn bộ Ledger.
     *
     * Delta = Total Debit - Total Credit
     *
     * Delta = 0
     * => ZERO_FINANCIAL_ERROR_PASSED
     *
     * Delta != 0
     * => FINANCIAL_ERROR_DETECTED
     */
    public Map<String, Object> reconcile() {

        BigDecimal totalDebit =
                getTotalDebit();

        BigDecimal totalCredit =
                getTotalCredit();

        BigDecimal delta =
                totalDebit.subtract(totalCredit);

        boolean passed =
                delta.compareTo(BigDecimal.ZERO) == 0;

        Map<String, Object> result =
                new HashMap<>();

        result.put(
                "timestamp",
                Instant.now()
        );

        result.put(
                "totalDebit",
                totalDebit
        );

        result.put(
                "totalCredit",
                totalCredit
        );

        result.put(
                "delta",
                delta
        );

        result.put(
                "status",
                passed
                        ? "ZERO_FINANCIAL_ERROR_PASSED"
                        : "FINANCIAL_ERROR_DETECTED"
        );

        result.put(
                "passed",
                passed
        );

        return result;
    }

    private BigDecimal getTotalDebit() {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM ledger_entries
                WHERE direction = 'DEBIT'
                """;

        BigDecimal result =
                jdbcTemplate.queryForObject(
                        sql,
                        BigDecimal.class
                );

        return result != null
                ? result
                : BigDecimal.ZERO;
    }

    private BigDecimal getTotalCredit() {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM ledger_entries
                WHERE direction = 'CREDIT'
                """;

        BigDecimal result =
                jdbcTemplate.queryForObject(
                        sql,
                        BigDecimal.class
                );

        return result != null
                ? result
                : BigDecimal.ZERO;
    }
}
