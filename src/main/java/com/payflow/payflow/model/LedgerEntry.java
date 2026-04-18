package com.payflow.payflow.model;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
@Data
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String accountId;      // who this entry belongs to

    @Enumerated(EnumType.STRING)
    private EntryType entryType;   // DEBIT or CREDIT

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum EntryType {
        DEBIT, CREDIT
    }
}