package com.payflow.payflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private long totalTransactions;
    private long successfulTransactions;
    private long failedTransactions;
    private long pendingTransactions;
    private BigDecimal totalVolume;
    private long cardPayments;
    private long upiPayments;
    private double successRate;        // percentage
}