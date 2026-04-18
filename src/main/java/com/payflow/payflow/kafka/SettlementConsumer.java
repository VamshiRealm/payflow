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
public class SettlementConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment.success", groupId = "settlement-group")
    public void handleSettlement(String message) {
        try {
            Map<?, ?> event = objectMapper.readValue(message, Map.class);
            log.info("SETTLEMENT → Merchant {} to receive ₹{} for transaction: {}",
                event.get("merchantId"), event.get("amount"), event.get("transactionId"));
        } catch (Exception e) {
            log.error("Failed to parse settlement event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.refund", groupId = "settlement-group")
    public void handleRefundSettlement(String message) {
        try {
            Map<?, ?> event = objectMapper.readValue(message, Map.class);
            log.info("SETTLEMENT → Reversing ₹{} from merchant {} for transaction: {}",
                event.get("amount"), event.get("merchantId"), event.get("transactionId"));
        } catch (Exception e) {
            log.error("Failed to parse refund settlement event: {}", e.getMessage());
        }
    }
}