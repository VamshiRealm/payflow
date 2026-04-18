package com.payflow.payflow.dto;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    private BigDecimal amount;    // partial refund supported
    private String reason;
}