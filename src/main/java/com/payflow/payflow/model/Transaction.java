package com.payflow.payflow.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String description;

    // Card specific fields
    private String maskedCardNumber;  // store only last 4 digits e.g. ****1234
    private String cardHolderName;

    // Idempotency
    @Column(unique = true)
    private String idempotencyKey;

    // Risk
    private Integer riskScore;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum PaymentMethod {
        CARD, UPI
    }

    public enum TransactionStatus {
        INITIATED, AUTHORIZED, CAPTURED, FAILED, REFUNDED, PENDING
    }
}