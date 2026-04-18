package com.payflow.payflow.service;

import com.payflow.payflow.dto.*;
import com.payflow.payflow.kafka.PaymentEventProducer;
import com.payflow.payflow.model.Transaction;
import com.payflow.payflow.model.UpiTransaction;
import com.payflow.payflow.repository.TransactionRepository;
import com.payflow.payflow.repository.UpiTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpiPaymentService {

    private final TransactionRepository transactionRepository;
    private final UpiTransactionRepository upiTransactionRepository;
    private final UpiValidatorService upiValidatorService;
    private final FraudService fraudService;
    private final LedgerService ledgerService;
    private final PaymentEventProducer eventProducer;      // ← must be here

    public UpiStatusResponse initiateCollect(UpiCollectRequest request, String initiatorId) {

        if (!upiValidatorService.isValidVpa(request.getPayerVpa())) {
            return new UpiStatusResponse(null, null, "FAILED", "COLLECT",
                request.getAmount(), request.getPayerVpa(), request.getPayeeVpa(),
                null, "Invalid payer VPA: " + request.getPayerVpa());
        }
        if (!upiValidatorService.isValidVpa(request.getPayeeVpa())) {
            return new UpiStatusResponse(null, null, "FAILED", "COLLECT",
                request.getAmount(), request.getPayerVpa(), request.getPayeeVpa(),
                null, "Invalid payee VPA: " + request.getPayeeVpa());
        }

        int riskScore = fraudService.calculateRiskScore(initiatorId, request.getAmount());
        if (fraudService.isFraudulent(riskScore)) {
            eventProducer.publishFraudAlert(                    // ← Kafka fraud alert
                null, initiatorId, request.getAmount(),
                riskScore, "High risk UPI collect request"
            );
            return new UpiStatusResponse(null, null, "FAILED", "COLLECT",
                request.getAmount(), request.getPayerVpa(), request.getPayeeVpa(),
                null, "Transaction flagged as fraudulent");
        }

        Transaction transaction = new Transaction();
        transaction.setCustomerId(request.getPayerVpa());
        transaction.setMerchantId(request.getMerchantId());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentMethod(Transaction.PaymentMethod.UPI);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transaction.setDescription(request.getRemarks());
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        transaction.setRiskScore(riskScore);
        transactionRepository.save(transaction);

        UpiTransaction upiTxn = new UpiTransaction();
        upiTxn.setTransactionId(transaction.getId());
        upiTxn.setPayerVpa(request.getPayerVpa());
        upiTxn.setPayeeVpa(request.getPayeeVpa());
        upiTxn.setAmount(request.getAmount());
        upiTxn.setFlowType(UpiTransaction.UpiFlowType.COLLECT);
        upiTxn.setUpiStatus(UpiTransaction.UpiStatus.PENDING);
        upiTxn.setRemarks(request.getRemarks());
        upiTxn.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        upiTransactionRepository.save(upiTxn);

        return new UpiStatusResponse(
            transaction.getId(), upiTxn.getId(),
            "PENDING", "COLLECT", request.getAmount(),
            request.getPayerVpa(), request.getPayeeVpa(),
            null, "Collect request sent. Waiting for approval."
        );
    }

    public UpiStatusResponse verifyCollect(String upiTransactionId, boolean approved) {

        UpiTransaction upiTxn = upiTransactionRepository.findById(upiTransactionId)
                .orElseThrow(() -> new RuntimeException("UPI transaction not found"));

        if (LocalDateTime.now().isAfter(upiTxn.getExpiresAt())) {
            upiTxn.setUpiStatus(UpiTransaction.UpiStatus.EXPIRED);
            upiTransactionRepository.save(upiTxn);
            return new UpiStatusResponse(
                upiTxn.getTransactionId(), upiTxn.getId(),
                "EXPIRED", "COLLECT", upiTxn.getAmount(),
                upiTxn.getPayerVpa(), upiTxn.getPayeeVpa(),
                null, "Collect request has expired"
            );
        }

        if (upiTxn.getUpiStatus() != UpiTransaction.UpiStatus.PENDING) {
            return new UpiStatusResponse(
                upiTxn.getTransactionId(), upiTxn.getId(),
                upiTxn.getUpiStatus().name(), "COLLECT", upiTxn.getAmount(),
                upiTxn.getPayerVpa(), upiTxn.getPayeeVpa(),
                upiTxn.getBankReferenceId(), "Already processed"
            );
        }

        Transaction transaction = transactionRepository.findById(upiTxn.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!approved) {
            upiTxn.setUpiStatus(UpiTransaction.UpiStatus.DECLINED);
            upiTxn.setUpdatedAt(LocalDateTime.now());
            upiTransactionRepository.save(upiTxn);
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            return new UpiStatusResponse(
                transaction.getId(), upiTxn.getId(),
                "DECLINED", "COLLECT", upiTxn.getAmount(),
                upiTxn.getPayerVpa(), upiTxn.getPayeeVpa(),
                null, "Customer declined the payment request"
            );
        }

        String bankRef = upiValidatorService.generateBankReferenceId();
        upiTxn.setUpiStatus(UpiTransaction.UpiStatus.SUCCESS);
        upiTxn.setBankReferenceId(bankRef);
        upiTxn.setUpdatedAt(LocalDateTime.now());
        upiTransactionRepository.save(upiTxn);

        transaction.setStatus(Transaction.TransactionStatus.CAPTURED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        ledgerService.recordPayment(
            transaction.getId(),
            upiTxn.getPayerVpa(),
            transaction.getMerchantId(),
            upiTxn.getAmount()
        );

        eventProducer.publishPaymentSuccess(transaction);      // ← Kafka success event

        return new UpiStatusResponse(
            transaction.getId(), upiTxn.getId(),
            "SUCCESS", "COLLECT", upiTxn.getAmount(),
            upiTxn.getPayerVpa(), upiTxn.getPayeeVpa(),
            bankRef, "Payment successful"
        );
    }

    public UpiStatusResponse initiateIntent(UpiIntentRequest request, String customerId) {

        if (!upiValidatorService.isValidVpa(request.getPayerVpa())) {
            return new UpiStatusResponse(null, null, "FAILED", "INTENT",
                request.getAmount(), request.getPayerVpa(), request.getPayeeVpa(),
                null, "Invalid payer VPA");
        }
        if (!upiValidatorService.isValidVpa(request.getPayeeVpa())) {
            return new UpiStatusResponse(null, null, "FAILED", "INTENT",
                request.getAmount(), request.getPayerVpa(), request.getPayeeVpa(),
                null, "Invalid payee VPA");
        }
        if (!upiValidatorService.hasSufficientBalance(request.getPayerVpa(), request.getAmount())) {
            return new UpiStatusResponse(null, null, "FAILED", "INTENT",
                request.getAmount(), request.getPayerVpa(), request.getPayeeVpa(),
                null, "Insufficient balance");
        }

        int riskScore = fraudService.calculateRiskScore(customerId, request.getAmount());
        if (fraudService.isFraudulent(riskScore)) {
            eventProducer.publishFraudAlert(                    // ← Kafka fraud alert
                null, customerId, request.getAmount(),
                riskScore, "High risk UPI intent"
            );
            return new UpiStatusResponse(null, null, "FAILED", "INTENT",
                request.getAmount(), request.getPayerVpa(), request.getPayeeVpa(),
                null, "Transaction flagged as fraudulent");
        }

        Transaction transaction = new Transaction();
        transaction.setCustomerId(customerId);
        transaction.setMerchantId(request.getMerchantId());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentMethod(Transaction.PaymentMethod.UPI);
        transaction.setStatus(Transaction.TransactionStatus.CAPTURED);
        transaction.setDescription(request.getRemarks());
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        transaction.setRiskScore(riskScore);
        transactionRepository.save(transaction);

        String bankRef = upiValidatorService.generateBankReferenceId();
        UpiTransaction upiTxn = new UpiTransaction();
        upiTxn.setTransactionId(transaction.getId());
        upiTxn.setPayerVpa(request.getPayerVpa());
        upiTxn.setPayeeVpa(request.getPayeeVpa());
        upiTxn.setAmount(request.getAmount());
        upiTxn.setFlowType(UpiTransaction.UpiFlowType.INTENT);
        upiTxn.setUpiStatus(UpiTransaction.UpiStatus.SUCCESS);
        upiTxn.setBankReferenceId(bankRef);
        upiTxn.setRemarks(request.getRemarks());
        upiTransactionRepository.save(upiTxn);

        ledgerService.recordPayment(
            transaction.getId(), customerId,
            request.getMerchantId(), request.getAmount()
        );

        eventProducer.publishPaymentSuccess(transaction);      // ← Kafka success event

        return new UpiStatusResponse(
            transaction.getId(), upiTxn.getId(),
            "SUCCESS", "INTENT", request.getAmount(),
            request.getPayerVpa(), request.getPayeeVpa(),
            bankRef, "UPI payment successful"
        );
    }

    public UpiStatusResponse getStatus(String transactionId) {
        UpiTransaction upiTxn = upiTransactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        return new UpiStatusResponse(
            transactionId, upiTxn.getId(),
            upiTxn.getUpiStatus().name(),
            upiTxn.getFlowType().name(),
            upiTxn.getAmount(),
            upiTxn.getPayerVpa(), upiTxn.getPayeeVpa(),
            upiTxn.getBankReferenceId(), "Status fetched"
        );
    }
}