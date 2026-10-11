package com.example.notificationauditservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
public class ReconciliationController {

    private final FinancialReconciliationService
            reconciliationService;

    public ReconciliationController(
            FinancialReconciliationService reconciliationService
    ) {
        this.reconciliationService =
                reconciliationService;
    }

    @GetMapping("/reconcile")
    public ResponseEntity<Map<String, Object>>
    reconcile() {

        Map<String, Object> result =
                reconciliationService.reconcile();

        return ResponseEntity.ok(result);
    }
}

