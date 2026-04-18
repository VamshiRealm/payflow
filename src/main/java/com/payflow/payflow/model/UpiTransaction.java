package com.payflow.payflow.model;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "upi_transactions")
@Data
public class UpiTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String transactionId;       // links to main transactions table

    @Column(nullable = false)
    private String payerVpa;            // e.g. rahul@upi

    @Column(nullable = false)
    private String payeeVpa;            // e.g. merchant@upi

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private UpiFlowType flowType;       // COLLECT or INTENT

    @Enumerated(EnumType.STRING)
    private UpiStatus upiStatus;

    private String bankReferenceId;     // simulated bank ref
    private String remarks;
    private LocalDateTime expiresAt;    // collect requests expire

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum UpiFlowType {
        COLLECT, INTENT
    }

    public enum UpiStatus {
        PENDING,        // waiting for customer approval
        APPROVED,       // customer approved
        DECLINED,       // customer declined
        EXPIRED,        // collect request timed out
        SUCCESS,        // money moved
        FAILED          // bank failure
    }
}