package com.example.paymentgatewayintegrationservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhook")
public class BankWebhookController {

    private final BankWebhookService bankWebhookService;

    public BankWebhookController(
            BankWebhookService bankWebhookService
    ) {
        this.bankWebhookService = bankWebhookService;
    }

    @PostMapping("/bank")
    public ResponseEntity<?> handleBankWebhook(
            @RequestBody BankWebhookPayload payload
    ) {

        boolean processed =
                bankWebhookService.processWebhook(payload);

        if (!processed) {
            return ResponseEntity.ok(
                    Map.of(
                            "status", "ALREADY_PROCESSED",
                            "message", "Webhook has already been processed"
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "status", "SUCCESS",
                        "message", "Webhook processed successfully"
                )
        );
    }
}
