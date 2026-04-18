package com.payflow.payflow.service;

import org.springframework.stereotype.Service;

@Service
public class UpiValidatorService {

    // Simulates checking if a VPA is registered with a bank
    public boolean isValidVpa(String vpa) {
        if (vpa == null || !vpa.contains("@")) return false;

        String[] parts = vpa.split("@");
        if (parts.length != 2) return false;

        String handle = parts[0];
        String bank = parts[1];

        // Simulate valid bank handles
        boolean validBank = bank.equals("upi") ||
                           bank.equals("oksbi") ||
                           bank.equals("okhdfcbank") ||
                           bank.equals("okicici") ||
                           bank.equals("paytm") ||
                           bank.equals("ybl");

        boolean validHandle = handle != null && handle.length() >= 3;

        return validBank && validHandle;
    }

    // Simulates checking if customer has enough balance
    public boolean hasSufficientBalance(String vpa, java.math.BigDecimal amount) {
        // In real world → bank API call
        // Simulation: always true unless amount > 100000
        return amount.compareTo(new java.math.BigDecimal("100000")) <= 0;
    }

    // Generates a simulated bank reference ID
    public String generateBankReferenceId() {
        return "BANK" + System.currentTimeMillis();
    }
}