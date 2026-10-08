package com.bank.transactionservice.service.impl;

import com.bank.transactionservice.client.AccountServiceClient;
import com.bank.transactionservice.dto.event.TransactionCompletedEvent;
import com.bank.transactionservice.dto.internal.AccountTransferResponse;
import com.bank.transactionservice.dto.request.TransferRequest;
import com.bank.transactionservice.dto.response.TransferResponse;
import com.bank.transactionservice.entity.BankTransaction;
import com.bank.transactionservice.entity.TransactionStatus;
import com.bank.transactionservice.exception.AccountTransferException;
import com.bank.transactionservice.kafka.TransactionEventProducer;
import com.bank.transactionservice.repository.BankTransactionRepository;
import com.bank.transactionservice.service.IdempotencyService;
import com.bank.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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

        if (senderUserId.equals(request.getReceiverUserId())) {
            throw new IllegalArgumentException(
                    "Sender and receiver cannot be the same"
            );
        }

        // 2. Create Redis key
        String redisKey =
                "idempotency:" + senderUserId + ":" + idempotencyKey;

        // The transaction database is the durable source of truth.
        Optional<BankTransaction> existingTransaction =
                bankTransactionRepository
                        .findBySenderUserIdAndIdempotencyKey(
                                senderUserId,
                                idempotencyKey
                        );

        if (existingTransaction.isPresent()) {
            BankTransaction existing = existingTransaction.get();
            validateSameRequest(existing, request);

            if (existing.getStatus() != TransactionStatus.PENDING) {
                return returnStoredResult(redisKey, existing);
            }
        }

        // Redis is only a response cache. Never let a cache-only result
        // authorize a transfer when the durable transaction row is missing.
        if (existingTransaction.isEmpty()
                && idempotencyService.get(redisKey) != null) {
            throw new IllegalStateException(
                    "Cached result exists without a matching database transaction"
            );
        }

        // Serialize same-key processing. The database uniqueness constraint
        // remains the final guard if the Redis lease expires.
        String lockToken =
                idempotencyService.acquireLock(redisKey);

        if (lockToken == null) {
            throw new IllegalStateException(
                    "A transfer with this Idempotency-Key is already being processed"
            );
        }

        try {
            // Recheck after taking the lock. This also recovers a durable
            // PENDING row left by a timeout or process failure.
            existingTransaction =
                    bankTransactionRepository
                            .findBySenderUserIdAndIdempotencyKey(
                                    senderUserId,
                                    idempotencyKey
                            );

            if (existingTransaction.isPresent()) {
                BankTransaction existing = existingTransaction.get();
                validateSameRequest(existing, request);

                if (existing.getStatus() != TransactionStatus.PENDING) {
                    return returnStoredResult(redisKey, existing);
                }

                return completePendingTransfer(
                        existing,
                        request,
                        redisKey
                );
            }

            // Save PENDING in its own repository transaction. Since this
            // method is not @Transactional, save() commits before the remote
            // account call begins.
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

            try {
                transaction = bankTransactionRepository.save(transaction);
            } catch (DataIntegrityViolationException duplicateKey) {
                // Another request may have committed this idempotency key
                // while our Redis lease was expiring. Reuse its reference.
                Optional<BankTransaction> winner =
                        bankTransactionRepository
                                .findBySenderUserIdAndIdempotencyKey(
                                        senderUserId,
                                        idempotencyKey
                                );

                if (winner.isEmpty()) {
                    throw duplicateKey;
                }

                transaction = winner.get();
                validateSameRequest(transaction, request);

                if (transaction.getStatus() != TransactionStatus.PENDING) {
                    return returnStoredResult(redisKey, transaction);
                }
            }

            return completePendingTransfer(
                    transaction,
                    request,
                    redisKey
            );

        } finally {
            // A Redis outage after the durable transaction completed must not
            // turn a successful transfer into an apparent failure.
            try {
                idempotencyService.releaseLock(redisKey, lockToken);
            } catch (RuntimeException ex) {
                log.warn(
                        "Could not release idempotency lock for key {}",
                        redisKey,
                        ex
                );
            }
        }
    }

    private TransferResponse completePendingTransfer(
            BankTransaction transaction,
            TransferRequest request,
            String redisKey) {

        try {
            AccountTransferResponse accountResponse =
                    accountServiceClient.transfer(
                            transaction.getReferenceId(),
                            transaction.getSenderUserId(),
                            transaction.getReceiverUserId(),
                            transaction.getAmount()
                    );

            validateAccountTransferResponse(transaction, accountResponse);

        } catch (AccountTransferException ex) {
            if (ex.isDefinitiveRejection()) {
                transaction.setStatus(TransactionStatus.FAILED);
                bankTransactionRepository.save(transaction);
            }

            // Network errors and 5xx responses leave the committed row
            // PENDING. A retry will call User Service with this same reference.
            throw ex;
        }

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setCompletedAt(LocalDateTime.now());

        // Commit SUCCESS before attempting Kafka. Kafka is not part of the
        // financial result and cannot roll this database update back.
        BankTransaction completed =
                bankTransactionRepository.save(transaction);

        publishCompletionEventBestEffort(completed);

        TransferResponse response = toResponse(completed);
        cacheResponseBestEffort(redisKey, response);

        return response;
    }

    private void validateAccountTransferResponse(
            BankTransaction transaction,
            AccountTransferResponse response) {

        boolean valid = response != null
                && Objects.equals(
                        transaction.getReferenceId(),
                        response.getTransferReference()
                )
                && Objects.equals(
                        transaction.getSenderUserId(),
                        response.getSenderUserId()
                )
                && Objects.equals(
                        transaction.getReceiverUserId(),
                        response.getReceiverUserId()
                )
                && response.getSenderBalance() != null
                && response.getReceiverBalance() != null
                && Objects.equals(
                        transaction.getCurrency(),
                        response.getCurrency()
                );

        if (!valid) {
            throw new AccountTransferException(
                    "Account service returned an incomplete or mismatched transfer result",
                    new IllegalStateException("Transfer result could not be verified")
            );
        }
    }

    private void publishCompletionEventBestEffort(
            BankTransaction transaction) {

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

        try {
            transactionEventProducer.publishTransactionCompleted(event);
        } catch (RuntimeException ex) {
            log.error(
                    "Transfer {} completed, but its Kafka notification event could not be published",
                    transaction.getReferenceId(),
                    ex
            );
        }
    }

    private TransferResponse returnStoredResult(
            String redisKey,
            BankTransaction transaction) {

        TransferResponse response = toResponse(transaction);
        cacheResponseBestEffort(redisKey, response);
        return response;
    }

    private void cacheResponseBestEffort(
            String redisKey,
            TransferResponse response) {

        try {
            idempotencyService.save(redisKey, response);
        } catch (RuntimeException ex) {
            log.warn(
                    "Could not cache transfer response for idempotency key {}",
                    redisKey,
                    ex
            );
        }
    }

    private void validateSameRequest(
            BankTransaction transaction,
            TransferRequest request) {

        if (!isSameRequest(transaction, request)) {
            throw new IllegalStateException(
                    "Idempotency key was already used with different request details"
            );
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
