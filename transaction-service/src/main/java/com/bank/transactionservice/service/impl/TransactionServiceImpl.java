package com.bank.transactionservice.service.impl;

import com.bank.transactionservice.client.AccountServiceClient;
import com.bank.transactionservice.dto.event.TransactionCompletedEvent;
import com.bank.transactionservice.dto.request.TransferRequest;
import com.bank.transactionservice.dto.response.IdempotencyResponse;
import com.bank.transactionservice.dto.response.TransferResponse;
import com.bank.transactionservice.entity.BankTransaction;
import com.bank.transactionservice.entity.TransactionStatus;
import com.bank.transactionservice.exception.AccountTransferException;
import com.bank.transactionservice.kafka.TransactionEventProducer;
import com.bank.transactionservice.repository.BankTransactionRepository;
import com.bank.transactionservice.service.IdempotencyService;
import com.bank.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl
        implements TransactionService {

    private final BankTransactionRepository
            bankTransactionRepository;

    private final AccountServiceClient
            accountServiceClient;

    private final IdempotencyService idempotencyService;

    private final TransactionEventProducer transactionEventProducer;


    @Override
    @Transactional(readOnly = true)
    public Page<TransferResponse> getAllTransactions(
            org.springframework.data.domain.Pageable pageable) {

        return bankTransactionRepository
                .findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getMyTransactions(Long userId) {

        List<BankTransaction> transactions =
                bankTransactionRepository.findUserTransactions(userId);

        return transactions.stream()
                .map(this::toResponse)
                .toList();
    }
    @Override
    @Transactional(
            noRollbackFor = AccountTransferException.class
    )
    public TransferResponse transfer(
            Long senderUserId,
            TransferRequest request,
            String idempotencyKey) {

        // 1. Validate idempotency key
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key is required"
            );
        }

        if (idempotencyKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Idempotency-Key cannot exceed 100 characters"
            );
        }

        // 2. Create Redis keys
        String redisKey =
                "idempotency:" + senderUserId + ":" + idempotencyKey;

        // 3. Check MySQL first: it is the source of truth
        Optional<BankTransaction> existingTransaction =
                bankTransactionRepository
                        .findBySenderUserIdAndIdempotencyKey(
                                senderUserId,
                                idempotencyKey
                        );

        if (existingTransaction.isPresent()) {

            BankTransaction existing = existingTransaction.get();

            if (!isSameRequest(existing, request)) {
                throw new IllegalStateException(
                        "Idempotency key was already used with different request details"
                );
            }

            if (existing.getStatus() == TransactionStatus.PENDING) {
                throw new IllegalStateException(
                        "Transaction is still being processed"
                );
            }

            TransferResponse response = toResponse(existing);

            idempotencyService.save(redisKey, response);

            return response;
        }

        // 4. Check Redis cache
        TransferResponse cachedResponse =
                idempotencyService.get(redisKey);

        if (cachedResponse != null) {
            throw new IllegalStateException(
                    "Cached result exists without a matching database transaction"
            );
        }

        // 5. Acquire lock
        String lockToken =
                idempotencyService.acquireLock(redisKey);

        if (lockToken == null) {
            throw new IllegalStateException(
                    "A transfer with this Idempotency-Key is already being processed"
            );
        }

        try {

            // 6. Recheck MySQL after acquiring lock
            existingTransaction =
                    bankTransactionRepository
                            .findBySenderUserIdAndIdempotencyKey(
                                    senderUserId,
                                    idempotencyKey
                            );

            if (existingTransaction.isPresent()) {

                BankTransaction existing = existingTransaction.get();

                if (!isSameRequest(existing, request)) {
                    throw new IllegalStateException(
                            "Idempotency key was already used with different request details"
                    );
                }

                if (existing.getStatus() == TransactionStatus.PENDING) {
                    throw new IllegalStateException(
                            "Transaction is still being processed"
                    );
                }

                TransferResponse response = toResponse(existing);

                idempotencyService.save(redisKey, response);

                return response;
            }

            // 7. Validate receiver
            if (senderUserId.equals(request.getReceiverUserId())) {
                throw new IllegalArgumentException(
                        "Sender and receiver cannot be the same"
                );
            }

            // 8. Create transaction
            String referenceId = "TXN-" + UUID.randomUUID();

            BankTransaction transaction =
                    BankTransaction.builder()
                            .senderUserId(senderUserId)
                            .receiverUserId(request.getReceiverUserId())
                            .amount(request.getAmount())
                            .currency("INR")
                            .status(TransactionStatus.PENDING)
                            .referenceId(referenceId)
                            .idempotencyKey(idempotencyKey)
                            .description(request.getDescription())
                            .createdAt(LocalDateTime.now())
                            .build();

            transaction = bankTransactionRepository.save(transaction);

            // 9. Call User Service
            try {

                accountServiceClient.transfer(
                        transaction.getReferenceId(),
                        senderUserId,
                        request.getReceiverUserId(),
                        request.getAmount()
                );

            } catch (AccountTransferException ex) {

                transaction.setStatus(TransactionStatus.FAILED);

                bankTransactionRepository.save(transaction);

                throw ex;
            }

            // 10. Mark success
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());

            transaction = bankTransactionRepository.save(transaction);

            // 11. Publish event
            TransactionCompletedEvent event =
                    TransactionCompletedEvent.builder()
                            .transactionId(transaction.getId())
                            .referenceId(transaction.getReferenceId())
                            .senderUserId(transaction.getSenderUserId())
                            .receiverUserId(transaction.getReceiverUserId())
                            .amount(transaction.getAmount())
                            .currency(transaction.getCurrency())
                            .description(transaction.getDescription())
                            .eventType("TRANSACTION_COMPLETED")
                            .build();

            transactionEventProducer.publishTransactionCompleted(event);

            // 12. Cache response
            TransferResponse response = toResponse(transaction);

            idempotencyService.save(redisKey, response);

            return response;

        } finally {

            // Release only if this request still owns the lock
            idempotencyService.releaseLock(redisKey, lockToken);
        }
    }

    private TransferResponse toResponse(
            BankTransaction transaction) {

            return TransferResponse.builder()
                    .transactionId(transaction.getId())
                    .referenceId(transaction.getReferenceId())
                    .senderUserId(transaction.getSenderUserId())
                    .receiverUserId(transaction.getReceiverUserId())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .status(transaction.getStatus().name())
                    .description(transaction.getDescription())
                    .createdAt(transaction.getCreatedAt())
                    .completedAt(transaction.getCompletedAt())
                    .build();

    }
    private boolean isSameRequest(
            BankTransaction transaction,
            TransferRequest request) {

        boolean sameReceiver =
                transaction.getReceiverUserId()
                        .equals(request.getReceiverUserId());

        boolean sameAmount =
                transaction.getAmount()
                        .compareTo(request.getAmount()) == 0;

        boolean sameDescription =
                java.util.Objects.equals(
                        transaction.getDescription(),
                        request.getDescription()
                );

        return sameReceiver && sameAmount && sameDescription;
    }
}