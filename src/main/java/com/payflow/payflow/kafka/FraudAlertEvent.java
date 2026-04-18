package com.payflow.payflow.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FraudAlertEvent {
    private String transactionId;
    private String customerId;
    private BigDecimal amount;
    private int riskScore;
    private String reason;
    private String timestamp;    // ← String instead of LocalDateTime
}