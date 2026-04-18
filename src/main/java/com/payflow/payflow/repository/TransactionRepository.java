package com.payflow.payflow.repository;



import com.payflow.payflow.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findByCustomerId(String customerId);
    List<Transaction> findByMerchantId(String merchantId);
}