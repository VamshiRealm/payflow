package com.payflow.payflow.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.success", groupId = "notification-group")
    public void handlePaymentSuccess(String message) {
        try {
            Map<?, ?> event = objectMapper.readValue(message, Map.class);
            log.info("NOTIFICATION → Payment of ₹{} successful for customer: {}. Txn ID: {}",
                event.get("amount"), event.get("customerId"), event.get("transactionId"));
        } catch (Exception e) {
            log.error("Failed to parse payment.success event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.failed", groupId = "notification-group")
    public void handlePaymentFailed(String message) {
        try {
            Map<?, ?> event = objectMapper.readValue(message, Map.class);
            log.info("NOTIFICATION → Payment of ₹{} FAILED for customer: {}. Reason: {}",
                event.get("amount"), event.get("customerId"), event.get("description"));
        } catch (Exception e) {
            log.error("Failed to parse payment.failed event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.refund", groupId = "notification-group")
    public void handleRefund(String message) {
        try {
            Map<?, ?> event = objectMapper.readValue(message, Map.class);
            log.info("NOTIFICATION → Refund of ₹{} processed for customer: {}. Txn ID: {}",
                event.get("amount"), event.get("customerId"), event.get("transactionId"));
        } catch (Exception e) {
            log.error("Failed to parse payment.refund event: {}", e.getMessage());
        }
    }
}