package com.payflow.payflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionHistoryResponse {
    private String transactionId;
    private String status;
    private String paymentMethod;
    private BigDecimal amount;
    private String merchantId;
    private String description;
    private String maskedCardNumber;  // null for UPI
    private String payerVpa;          // null for Card
    private String payeeVpa;          // null for Card
    private Integer riskScore;
    private LocalDateTime createdAt;
}