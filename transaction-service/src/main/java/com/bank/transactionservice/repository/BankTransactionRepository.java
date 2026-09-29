package com.bank.transactionservice.repository;

import com.bank.transactionservice.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByReferenceId(String referenceId);

    List<BankTransaction> findBySenderUserIdOrderByCreatedAtDesc(
            Long senderUserId);

    List<BankTransaction> findByReceiverUserIdOrderByCreatedAtDesc(
            Long receiverUserId);

    @Query("""
           SELECT t
           FROM BankTransaction t
           WHERE t.senderUserId = :userId
              OR t.receiverUserId = :userId
           ORDER BY t.createdAt DESC
           """)
    List<BankTransaction> findUserTransactions(Long userId);

    Optional<BankTransaction> findBySenderUserIdAndIdempotencyKey(
            Long senderUserId,
            String idempotencyKey);
}