package com.payflow.payflow.service;

import com.payflow.payflow.dto.CardPaymentRequest;
import com.payflow.payflow.dto.PaymentResponse;
import com.payflow.payflow.kafka.PaymentEventProducer;
import com.payflow.payflow.model.Transaction;
import com.payflow.payflow.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardPaymentService {

    private final TransactionRepository transactionRepository;
    private final FraudService fraudService;
    private final LedgerService ledgerService;
    private final PaymentEventProducer eventProducer;   // ← must be here

    public PaymentResponse initiatePayment(CardPaymentRequest request,
                                           String customerId,
                                           String idempotencyKey) {

        if (idempotencyKey != null) {
            Optional<Transaction> existing =
                transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Transaction t = existing.get();
                return new PaymentResponse(
                    t.getId(), t.getStatus().name(),
                    t.getAmount(), "Duplicate request — returning existing transaction",
                    t.getRiskScore()
                );
            }
        }

        if (!isValidCard(request.getCardNumber(), request.getExpiryYear(), request.getCvv())) {
            return new PaymentResponse(null, "FAILED", request.getAmount(), "Invalid card details", 0);
        }

        int riskScore = fraudService.calculateRiskScore(customerId, request.getAmount());
        if (fraudService.isFraudulent(riskScore)) {
            // Publish fraud alert
            eventProducer.publishFraudAlert(
                null, customerId, request.getAmount(),
                riskScore, "High risk card transaction"
            );
            return new PaymentResponse(null, "FAILED", request.getAmount(),
                "Transaction flagged as fraudulent", riskScore);
        }

        Transaction transaction = new Transaction();
        transaction.setCustomerId(customerId);
        transaction.setMerchantId(request.getMerchantId());
        transaction.setAmount(request.getAmount());
        transaction.setPaymentMethod(Transaction.PaymentMethod.CARD);
        transaction.setStatus(Transaction.TransactionStatus.AUTHORIZED);
        transaction.setDescription(request.getDescription());
        transaction.setMaskedCardNumber("****" + request.getCardNumber()
                .substring(request.getCardNumber().length() - 4));
        transaction.setCardHolderName(request.getCardHolderName());
        transaction.setIdempotencyKey(idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString());
        transaction.setRiskScore(riskScore);
        transactionRepository.save(transaction);

        ledgerService.recordPayment(
            transaction.getId(), customerId,
            request.getMerchantId(), request.getAmount()
        );

        transaction.setStatus(Transaction.TransactionStatus.CAPTURED);
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Publish success event to Kafka
        eventProducer.publishPaymentSuccess(transaction);

        return new PaymentResponse(
            transaction.getId(), "CAPTURED",
            transaction.getAmount(), "Payment successful", riskScore
        );
    }

    private boolean isValidCard(String cardNumber, String expiryYear, String cvv) {
        if (cardNumber == null || cardNumber.length() != 16) return false;
        if (cvv == null || cvv.length() != 3) return false;
        if (expiryYear == null) return false;
        int year = Integer.parseInt(expiryYear);
        int currentYear = LocalDateTime.now().getYear() % 100;
        return year >= currentYear;
    }
}