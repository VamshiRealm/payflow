package com.payflow.payflow.repository;


import com.payflow.payflow.model.UpiTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UpiTransactionRepository extends JpaRepository<UpiTransaction, String> {
    Optional<UpiTransaction> findByTransactionId(String transactionId);
    List<UpiTransaction> findByPayerVpa(String payerVpa);
    List<UpiTransaction> findByPayeeVpa(String payeeVpa);
}