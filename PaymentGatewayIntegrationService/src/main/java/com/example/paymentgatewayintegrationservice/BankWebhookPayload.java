package com.example.paymentgatewayintegrationservice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BankWebhookPayload {

    private String bankRefId;
    private String accountNumber;
    private BigDecimal amount;
    private String checksum;
}

