package com.example.accountcoreledgerservices;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, String> {
    Optional<TransactionRecord> findByIdempotencyKey(String idempotencyKey);
}
