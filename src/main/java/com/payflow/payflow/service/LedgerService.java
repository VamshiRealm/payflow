package com.payflow.payflow.service;

import com.payflow.payflow.model.LedgerEntry;
import com.payflow.payflow.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    // Every payment = 2 entries (double entry bookkeeping)
    public void recordPayment(String transactionId,
                               String customerId,
                               String merchantId,
                               BigDecimal amount) {

        // 1. Debit customer
        LedgerEntry debit = new LedgerEntry();
        debit.setTransactionId(transactionId);
        debit.setAccountId(customerId);
        debit.setEntryType(LedgerEntry.EntryType.DEBIT);
        debit.setAmount(amount);
        debit.setDescription("Payment debited");
        ledgerRepository.save(debit);

        // 2. Credit merchant
        LedgerEntry credit = new LedgerEntry();
        credit.setTransactionId(transactionId);
        credit.setAccountId(merchantId);
        credit.setEntryType(LedgerEntry.EntryType.CREDIT);
        credit.setAmount(amount);
        credit.setDescription("Payment credited");
        ledgerRepository.save(credit);
    }

    // Reverse entries for refund
    public void reversePayment(String transactionId,
                                String customerId,
                                String merchantId,
                                BigDecimal amount) {

        // Credit back to customer
        LedgerEntry credit = new LedgerEntry();
        credit.setTransactionId(transactionId + "_REFUND");
        credit.setAccountId(customerId);
        credit.setEntryType(LedgerEntry.EntryType.CREDIT);
        credit.setAmount(amount);
        credit.setDescription("Refund credited");
        ledgerRepository.save(credit);

        // Debit from merchant
        LedgerEntry debit = new LedgerEntry();
        debit.setTransactionId(transactionId + "_REFUND");
        debit.setAccountId(merchantId);
        debit.setEntryType(LedgerEntry.EntryType.DEBIT);
        debit.setAmount(amount);
        debit.setDescription("Refund debited");
        ledgerRepository.save(debit);
    }
}