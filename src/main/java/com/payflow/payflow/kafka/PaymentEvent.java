package com.payflow.payflow.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private String transactionId;
    private String customerId;
    private String merchantId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private Integer riskScore;
    private String description;
    private String timestamp;    // ← String instead of LocalDateTime
}