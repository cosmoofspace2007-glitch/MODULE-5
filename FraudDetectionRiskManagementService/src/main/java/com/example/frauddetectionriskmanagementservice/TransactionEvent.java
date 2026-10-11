package com.example.frauddetectionriskmanagementservice;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class TransactionEvent
{
    private String transactionId;
    private String accountId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String status;
    private Instant timestamp;
}