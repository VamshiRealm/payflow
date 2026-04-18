package com.payflow.payflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RefundResponse {
    private String transactionId;
    private String status;
    private BigDecimal refundedAmount;
    private String message;
}