package com.payflow.payflow.controller;

import com.payflow.payflow.dto.CardPaymentRequest;
import com.payflow.payflow.dto.PaymentResponse;
import com.payflow.payflow.service.CardPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/card")
@RequiredArgsConstructor
public class CardPaymentController {

    private final CardPaymentService cardPaymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestBody CardPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Get logged-in user's email (acts as their ID for now)
        String customerId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        PaymentResponse response = cardPaymentService.initiatePayment(
                request, customerId, idempotencyKey);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getPayment(@PathVariable String id) {
        return ResponseEntity.ok("Transaction ID: " + id);
    }
}