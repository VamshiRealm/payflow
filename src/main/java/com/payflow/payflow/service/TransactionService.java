package com.payflow.payflow.service;

import com.payflow.payflow.dto.*;
import com.payflow.payflow.kafka.PaymentEventProducer;
import com.payflow.payflow.model.LedgerEntry;
import com.payflow.payflow.model.Transaction;
import com.payflow.payflow.model.UpiTransaction;
import com.payflow.payflow.repository.LedgerRepository;
import com.payflow.payflow.repository.TransactionRepository;
import com.payflow.payflow.repository.UpiTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UpiTransactionRepository upiTransactionRepository;
    private final LedgerRepository ledgerRepository;
    private final LedgerService ledgerService;
    private final PaymentEventProducer eventProducer;      // ← Kafka wired in

    // ── GET ALL TRANSACTIONS FOR LOGGED IN USER ───────────────────
    public List<TransactionHistoryResponse> getMyTransactions(String customerId) {
        List<Transaction> transactions = transactionRepository.findByCustomerId(customerId);
        return transactions.stream()
                .map(t -> enrichTransaction(t))
                .collect(Collectors.toList());
    }

    // ── GET SINGLE TRANSACTION ────────────────────────────────────
    public TransactionHistoryResponse getTransactionById(String transactionId,
                                                          String requesterId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getCustomerId().equals(requesterId) &&
            !transaction.getMerchantId().equals(requesterId)) {
            throw new RuntimeException("Access denied");
        }

        return enrichTransaction(transaction);
    }

    // ── GET LEDGER ENTRIES FOR A TRANSACTION ──────────────────────
    public List<LedgerEntry> getLedgerEntries(String transactionId) {
        return ledgerRepository.findByTransactionId(transactionId);
    }

    // ── REFUND ────────────────────────────────────────────────────
    public RefundResponse refundTransaction(String transactionId,
                                            RefundRequest request,
                                            String requesterId) {

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getCustomerId().equals(requesterId) &&
            !transaction.getMerchantId().equals(requesterId)) {
            throw new RuntimeException("Access denied");
        }

        if (transaction.getStatus() != Transaction.TransactionStatus.CAPTURED) {
            return new RefundResponse(
                transactionId, "FAILED", BigDecimal.ZERO,
                "Only captured transactions can be refunded. Current status: "
                    + transaction.getStatus()
            );
        }

        BigDecimal refundAmount = request.getAmount() != null
                ? request.getAmount()
                : transaction.getAmount();

        if (refundAmount.compareTo(transaction.getAmount()) > 0) {
            return new RefundResponse(
                transactionId, "FAILED", BigDecimal.ZERO,
                "Refund amount cannot exceed original amount of " + transaction.getAmount()
            );
        }

        ledgerService.reversePayment(
            transaction.getId(),
            transaction.getCustomerId(),
            transaction.getMerchantId(),
            refundAmount
        );

        transaction.setStatus(Transaction.TransactionStatus.REFUNDED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Publish refund event to Kafka
        eventProducer.publishRefund(transaction, refundAmount);

        return new RefundResponse(
            transactionId, "REFUNDED", refundAmount,
            "Refund of ₹" + refundAmount + " processed successfully"
        );
    }

    // ── ANALYTICS SUMMARY ─────────────────────────────────────────
    public AnalyticsSummaryResponse getAnalyticsSummary(String userId) {
        List<Transaction> all = transactionRepository.findByCustomerId(userId);

        long total = all.size();
        long successful = all.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.CAPTURED)
                .count();
        long failed = all.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.FAILED)
                .count();
        long pending = all.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING)
                .count();

        BigDecimal totalVolume = all.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.CAPTURED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cardPayments = all.stream()
                .filter(t -> t.getPaymentMethod() == Transaction.PaymentMethod.CARD)
                .count();
        long upiPayments = all.stream()
                .filter(t -> t.getPaymentMethod() == Transaction.PaymentMethod.UPI)
                .count();

        double successRate = total == 0 ? 0 :
                BigDecimal.valueOf((double) successful / total * 100)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        return new AnalyticsSummaryResponse(
                total, successful, failed, pending,
                totalVolume, cardPayments, upiPayments, successRate
        );
    }

    // ── HELPER ────────────────────────────────────────────────────
    private TransactionHistoryResponse enrichTransaction(Transaction t) {
        String payerVpa = null;
        String payeeVpa = null;

        if (t.getPaymentMethod() == Transaction.PaymentMethod.UPI) {
            Optional<UpiTransaction> upiTxn =
                upiTransactionRepository.findByTransactionId(t.getId());
            if (upiTxn.isPresent()) {
                payerVpa = upiTxn.get().getPayerVpa();
                payeeVpa = upiTxn.get().getPayeeVpa();
            }
        }

        return new TransactionHistoryResponse(
            t.getId(), t.getStatus().name(),
            t.getPaymentMethod().name(), t.getAmount(),
            t.getMerchantId(), t.getDescription(),
            t.getMaskedCardNumber(), payerVpa, payeeVpa,
            t.getRiskScore(), t.getCreatedAt()
        );
    }
}