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

        /*
         * 1. Validate Idempotency-Key
         */
        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency-Key is required"
            );
        }

        if (idempotencyKey.length() > 100) {

            throw new IllegalArgumentException(
                    "Idempotency-Key cannot exceed 100 characters"
            );
        }


        /*
         * 2. Create Redis key
         */
        String redisKey =
                "idempotency:"
                        + senderUserId
                        + ":"
                        + idempotencyKey;


        /*
         * 3. Check Redis first
         */
        TransferResponse cachedResponse =
                idempotencyService.get(redisKey);

        if (cachedResponse != null) {

            return cachedResponse;
        }
        boolean lockAcquired =
                idempotencyService.acquireLock(redisKey);

        if (!lockAcquired) {

            throw new IllegalStateException(
                    "A transfer with this Idempotency-Key is already being processed"
            );
        }


        /*
         * 4. Check MySQL
         *
         * Redis may have expired or may not contain
         * the result, but the transaction could still
         * exist in the database.
         */
        Optional<BankTransaction> existingTransaction =
                bankTransactionRepository
                        .findBySenderUserIdAndIdempotencyKey(
                                senderUserId,
                                idempotencyKey
                        );

        if (existingTransaction.isPresent()) {

            TransferResponse response =
                    toResponse(existingTransaction.get());

            /*
             * Put the result back into Redis
             */
            idempotencyService.save(
                    redisKey,
                    response
            );

            return response;
        }


        /*
         * 5. Sender cannot transfer to himself
         */
        if (senderUserId.equals(
                request.getReceiverUserId())) {

            throw new IllegalArgumentException(
                    "Sender and receiver cannot be the same"
            );
        }


        /*
         * 6. Generate unique transaction reference
         */
        String referenceId =
                "TXN-" + UUID.randomUUID();


        /*
         * 7. Create PENDING transaction
         */
        BankTransaction transaction =
                BankTransaction.builder()
                        .senderUserId(senderUserId)
                        .receiverUserId(
                                request.getReceiverUserId()
                        )
                        .amount(request.getAmount())
                        .currency("INR")
                        .status(TransactionStatus.PENDING)
                        .referenceId(referenceId)

                        // IMPORTANT
                        .idempotencyKey(idempotencyKey)

                        .description(request.getDescription())
                        .createdAt(LocalDateTime.now())
                        .build();


        transaction =
                bankTransactionRepository.save(transaction);


        /*
         * 8. Ask User Service to move the money
         */
        try {

            accountServiceClient.transfer(
                    senderUserId,
                    request.getReceiverUserId(),
                    request.getAmount()
            );

        } catch (AccountTransferException ex) {

            transaction.setStatus(
                    TransactionStatus.FAILED
            );

            transaction =
                    bankTransactionRepository.save(transaction);

            /*
             * Release Redis processing lock
             * because the transfer failed.
             */
            idempotencyService.delete(redisKey);

            throw ex;
        }

        /*
         * 9. Account transfer succeeded
         *    → mark transaction SUCCESS
         */
        transaction.setStatus(
                TransactionStatus.SUCCESS
        );

        transaction.setCompletedAt(
                LocalDateTime.now()
        );


        /*
         * 10. Save SUCCESS transaction
         */
        transaction =
                bankTransactionRepository.save(transaction);

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


        /*
         * 11. Convert entity → response
         */
        TransferResponse response =
                toResponse(transaction);


        /*
         * 12. Cache successful response in Redis
         */
        idempotencyService.save(
                redisKey,
                response
        );


        /*
         * 13. Return response
         */
        return response;
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
}