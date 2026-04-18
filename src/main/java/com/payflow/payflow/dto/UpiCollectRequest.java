package com.payflow.payflow.dto;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpiCollectRequest {
    private String payerVpa;        // customer's UPI ID e.g. rahul@upi
    private String payeeVpa;        // merchant's UPI ID e.g. merchant@upi
    private BigDecimal amount;
    private String remarks;
    private String merchantId;
}