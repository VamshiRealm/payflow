package com.payflow.payflow.service;

import com.payflow.payflow.model.Transaction;
import com.payflow.payflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudService {

    private final TransactionRepository transactionRepository;

    // Returns risk score 0-100
    public int calculateRiskScore(String customerId, BigDecimal amount) {
    int score = 0;

    // Rule 1 — High value transaction
    if (amount.compareTo(new BigDecimal("50000")) > 0) {
        score += 50;                                        // ← was 40, now 50
    } else if (amount.compareTo(new BigDecimal("10000")) > 0) {
        score += 30;                                        // ← was 20, now 30
    } else if (amount.compareTo(new BigDecimal("5000")) > 0) {
        score += 15;
    }

    // Rule 2 — Rapid multiple transactions
    List<Transaction> recentTransactions = transactionRepository
            .findByCustomerId(customerId);

    long recentCount = recentTransactions.stream()
            .filter(t -> t.getCreatedAt()
                    .isAfter(LocalDateTime.now().minusMinutes(10)))
            .count();

    if (recentCount > 5) {
        score += 40;
    } else if (recentCount > 3) {
        score += 20;
    }

    return Math.min(score, 100);
}

    public boolean isFraudulent(int riskScore) {
        return riskScore >= 70;
    }
}