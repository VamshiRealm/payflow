package com.payflow.payflow.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpiIntentRequest {
    private String payerVpa;        // customer's UPI ID
    private String payeeVpa;        // merchant's UPI ID
    private BigDecimal amount;
    private String remarks;
    private String merchantId;
}