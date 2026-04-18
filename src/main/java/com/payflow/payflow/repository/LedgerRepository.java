package com.payflow.payflow.repository;

import com.payflow.payflow.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, String> {
    List<LedgerEntry> findByTransactionId(String transactionId);
    List<LedgerEntry> findByAccountId(String accountId);
}