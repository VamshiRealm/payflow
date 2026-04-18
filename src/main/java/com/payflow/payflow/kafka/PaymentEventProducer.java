package com.payflow.payflow.kafka;

import com.payflow.payflow.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentSuccess(Transaction transaction) {
        PaymentEvent event = new PaymentEvent(
            transaction.getId(),
            transaction.getCustomerId(),
            transaction.getMerchantId(),
            transaction.getAmount(),
            transaction.getPaymentMethod().name(),
            "SUCCESS",
            transaction.getRiskScore(),
            transaction.getDescription(),
            LocalDateTime.now().toString()    // ← .toString()
        );
        kafkaTemplate.send("payment.success", transaction.getId(), event);
        log.info("Published payment.success for transaction: {}", transaction.getId());
    }

    public void publishPaymentFailed(Transaction transaction, String reason) {
        PaymentEvent event = new PaymentEvent(
            transaction.getId(),
            transaction.getCustomerId(),
            transaction.getMerchantId(),
            transaction.getAmount(),
            transaction.getPaymentMethod().name(),
            "FAILED",
            transaction.getRiskScore(),
            reason,
            LocalDateTime.now().toString()    // ← .toString()
        );
        kafkaTemplate.send("payment.failed", transaction.getId(), event);
        log.info("Published payment.failed for transaction: {}", transaction.getId());
    }

    public void publishRefund(Transaction transaction, BigDecimal refundAmount) {
        PaymentEvent event = new PaymentEvent(
            transaction.getId(),
            transaction.getCustomerId(),
            transaction.getMerchantId(),
            refundAmount,
            transaction.getPaymentMethod().name(),
            "REFUNDED",
            transaction.getRiskScore(),
            "Refund processed",
            LocalDateTime.now().toString()    // ← .toString()
        );
        kafkaTemplate.send("payment.refund", transaction.getId(), event);
        log.info("Published payment.refund for transaction: {}", transaction.getId());
    }

    public void publishFraudAlert(String transactionId, String customerId,
                                   BigDecimal amount, int riskScore, String reason) {
        FraudAlertEvent event = new FraudAlertEvent(
            transactionId, customerId,
            amount, riskScore, reason,
            LocalDateTime.now().toString()    // ← .toString()
        );
        kafkaTemplate.send("fraud.alert", transactionId, event);
        log.warn("Published fraud.alert for customer: {} risk score: {}",
            customerId, riskScore);
    }
}