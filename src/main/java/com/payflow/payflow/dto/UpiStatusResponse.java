package com.payflow.payflow.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UpiStatusResponse {
    private String transactionId;
    private String upiTransactionId;
    private String upiStatus;
    private String flowType;
    private BigDecimal amount;
    private String payerVpa;
    private String payeeVpa;
    private String bankReferenceId;
    private String message;
}