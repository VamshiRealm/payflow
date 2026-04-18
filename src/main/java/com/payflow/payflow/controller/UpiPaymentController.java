package com.payflow.payflow.controller;


import com.payflow.payflow.dto.*;
import com.payflow.payflow.service.UpiPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/payments/upi")
@RequiredArgsConstructor
public class UpiPaymentController {

    private final UpiPaymentService upiPaymentService;

    // Merchant initiates collect request
    @PostMapping("/collect")
    public ResponseEntity<UpiStatusResponse> initiateCollect(
            @RequestBody UpiCollectRequest request) {
        String initiatorId = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(upiPaymentService.initiateCollect(request, initiatorId));
    }

    // Customer approves or declines collect request
    @PostMapping("/verify")
    public ResponseEntity<UpiStatusResponse> verifyCollect(
            @RequestBody Map<String, Object> body) {
        String upiTransactionId = (String) body.get("upiTransactionId");
        boolean approved = (Boolean) body.get("approved");
        return ResponseEntity.ok(upiPaymentService.verifyCollect(upiTransactionId, approved));
    }

    // Customer pushes payment directly
    @PostMapping("/intent")
    public ResponseEntity<UpiStatusResponse> initiateIntent(
            @RequestBody UpiIntentRequest request) {
        String customerId = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return ResponseEntity.ok(upiPaymentService.initiateIntent(request, customerId));
    }

    // Check status of any UPI transaction
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<UpiStatusResponse> getStatus(
            @PathVariable String transactionId) {
        return ResponseEntity.ok(upiPaymentService.getStatus(transactionId));
    }
}