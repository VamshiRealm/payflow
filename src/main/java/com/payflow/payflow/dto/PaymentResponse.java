package com.payflow.payflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentResponse {
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String message;
    private Integer riskScore;
}