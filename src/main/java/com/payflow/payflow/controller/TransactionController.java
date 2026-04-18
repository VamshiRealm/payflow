package com.payflow.payflow.controller;

import com.payflow.payflow.dto.*;
import com.payflow.payflow.model.LedgerEntry;
import com.payflow.payflow.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    private String getCurrentUser() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // GET /transactions — full history
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionHistoryResponse>> getMyTransactions() {
        return ResponseEntity.ok(transactionService.getMyTransactions(getCurrentUser()));
    }

    // GET /transactions/{id} — single transaction
    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionHistoryResponse> getTransaction(
            @PathVariable String id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id, getCurrentUser()));
    }

    // GET /transactions/{id}/ledger — ledger entries
    @GetMapping("/transactions/{id}/ledger")
    public ResponseEntity<List<LedgerEntry>> getLedger(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.getLedgerEntries(id));
    }

    // POST /payments/{id}/refund — refund
    @PostMapping("/payments/{id}/refund")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable String id,
            @RequestBody RefundRequest request) {
        return ResponseEntity.ok(transactionService.refundTransaction(id, request, getCurrentUser()));
    }

    // GET /analytics/summary — stats
    @GetMapping("/analytics/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getAnalytics() {
        return ResponseEntity.ok(transactionService.getAnalyticsSummary(getCurrentUser()));
    }
}