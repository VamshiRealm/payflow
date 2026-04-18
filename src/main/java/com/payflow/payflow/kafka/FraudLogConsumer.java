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
public class FraudLogConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "fraud.alert", groupId = "fraud-group")
    public void handleFraudAlert(String message) {
        try {
            Map<?, ?> event = objectMapper.readValue(message, Map.class);
            log.warn("FRAUD ALERT → Customer: {} | Amount: ₹{} | Risk Score: {} | Reason: {}",
                event.get("customerId"), event.get("amount"),
                event.get("riskScore"), event.get("reason"));
        } catch (Exception e) {
            log.error("Failed to parse fraud alert event: {}", e.getMessage());
        }
    }
}