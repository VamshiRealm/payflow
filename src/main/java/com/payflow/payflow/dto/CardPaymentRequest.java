package com.payflow.payflow.dto;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardPaymentRequest {
    private String cardNumber;      // 16 digit card number
    private String cardHolderName;
    private String expiryMonth;     // MM
    private String expiryYear;      // YY
    private String cvv;
    private BigDecimal amount;
    private String merchantId;
    private String description;
}