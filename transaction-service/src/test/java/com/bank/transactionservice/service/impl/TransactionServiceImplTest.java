package com.bank.transactionservice.service.impl;

import com.bank.transactionservice.client.AccountServiceClient;
import com.bank.transactionservice.dto.internal.AccountTransferResponse;
import com.bank.transactionservice.dto.request.TransferRequest;
import com.bank.transactionservice.dto.response.TransferResponse;
import com.bank.transactionservice.entity.BankTransaction;
import com.bank.transactionservice.entity.TransactionStatus;
import com.bank.transactionservice.exception.AccountTransferException;
import com.bank.transactionservice.kafka.TransactionEventProducer;
import com.bank.transactionservice.repository.BankTransactionRepository;
import com.bank.transactionservice.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final Long SENDER_ID = 11L;
    private static final Long RECEIVER_ID = 22L;
    private static final String IDEMPOTENCY_KEY = "request-123";
    private static final String REDIS_KEY =
            "idempotency:" + SENDER_ID + ":" + IDEMPOTENCY_KEY;
    private static final String LOCK_TOKEN = "lock-owner-123";
    private static final BigDecimal AMOUNT = new BigDecimal("12.50");

    @Mock
    private BankTransactionRepository bankTransactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private TransactionEventProducer transactionEventProducer;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private final List<TransactionStatus> savedStatuses = new ArrayList<>();

    @BeforeEach
    void recordStatusesSavedByTheRepository() {
        when(bankTransactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> {
                    BankTransaction transaction = invocation.getArgument(0);
                    if (transaction.getId() == null) {
                        transaction.setId(100L);
                    }
                    savedStatuses.add(transaction.getStatus());
                    return transaction;
                });
    }

    @Test
    void savesPendingBeforeCallingAccountsAndKeepsSuccessWhenKafkaFails() {
        stubNewRequest();
        when(accountServiceClient.transfer(
                anyString(),
                eq(SENDER_ID),
                eq(RECEIVER_ID),
                eq(AMOUNT)
        )).thenAnswer(invocation -> accountResponse(invocation.getArgument(0)));
        doThrow(new RuntimeException("broker unavailable"))
                .when(transactionEventProducer)
                .publishTransactionCompleted(any());

        TransferResponse response = transactionService.transfer(
                SENDER_ID,
                request(),
                IDEMPOTENCY_KEY
        );

        assertEquals("SUCCESS", response.getStatus());
        assertEquals(
                List.of(TransactionStatus.PENDING, TransactionStatus.SUCCESS),
                savedStatuses
        );

        InOrder order = inOrder(bankTransactionRepository, accountServiceClient,
                transactionEventProducer);
        order.verify(bankTransactionRepository).save(any(BankTransaction.class));
        order.verify(accountServiceClient).transfer(
                anyString(), eq(SENDER_ID), eq(RECEIVER_ID), eq(AMOUNT)
        );
        order.verify(bankTransactionRepository).save(any(BankTransaction.class));
        order.verify(transactionEventProducer).publishTransactionCompleted(any());

        verify(idempotencyService).save(eq(REDIS_KEY), any(TransferResponse.class));
    }

    @Test
    void retriesPendingTransferUsingItsExistingReference() {
        BankTransaction pending = transaction(
                77L,
                "TXN-stable-reference",
                TransactionStatus.PENDING
        );
        when(bankTransactionRepository.findBySenderUserIdAndIdempotencyKey(
                SENDER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(Optional.of(pending), Optional.of(pending));
        when(idempotencyService.acquireLock(REDIS_KEY)).thenReturn(LOCK_TOKEN);
        when(accountServiceClient.transfer(
                eq("TXN-stable-reference"),
                eq(SENDER_ID),
                eq(RECEIVER_ID),
                eq(AMOUNT)
        )).thenReturn(accountResponse("TXN-stable-reference"));

        TransferResponse response = transactionService.transfer(
                SENDER_ID,
                request(),
                IDEMPOTENCY_KEY
        );

        assertEquals("SUCCESS", response.getStatus());
        verify(accountServiceClient).transfer(
                "TXN-stable-reference",
                SENDER_ID,
                RECEIVER_ID,
                AMOUNT
        );
        assertEquals(List.of(TransactionStatus.SUCCESS), savedStatuses);
    }

    @Test
    void leavesTransferPendingAfterAnUncertainHttpFailure() {
        stubNewRequest();
        when(accountServiceClient.transfer(
                anyString(),
                eq(SENDER_ID),
                eq(RECEIVER_ID),
                eq(AMOUNT)
        )).thenThrow(new AccountTransferException(
                "Account transfer failed",
                new ResourceAccessException("request timed out")
        ));

        assertThrows(
                AccountTransferException.class,
                () -> transactionService.transfer(
                        SENDER_ID,
                        request(),
                        IDEMPOTENCY_KEY
                )
        );

        assertEquals(List.of(TransactionStatus.PENDING), savedStatuses);
        verify(transactionEventProducer, never())
                .publishTransactionCompleted(any());
        verify(idempotencyService).releaseLock(REDIS_KEY, LOCK_TOKEN);
    }

    @Test
    void marksFailedOnlyWhenAccountServiceDefinitivelyRejectsRequest() {
        stubNewRequest();
        when(accountServiceClient.transfer(
                anyString(),
                eq(SENDER_ID),
                eq(RECEIVER_ID),
                eq(AMOUNT)
        )).thenThrow(new AccountTransferException(
                "Account transfer failed",
                new HttpClientErrorException(HttpStatus.CONFLICT)
        ));

        AccountTransferException exception = assertThrows(
                AccountTransferException.class,
                () -> transactionService.transfer(
                        SENDER_ID,
                        request(),
                        IDEMPOTENCY_KEY
                )
        );

        assertEquals(true, exception.isDefinitiveRejection());
        assertEquals(
                List.of(TransactionStatus.PENDING, TransactionStatus.FAILED),
                savedStatuses
        );
    }

    private void stubNewRequest() {
        when(bankTransactionRepository.findBySenderUserIdAndIdempotencyKey(
                SENDER_ID,
                IDEMPOTENCY_KEY
        )).thenReturn(Optional.empty(), Optional.empty());
        when(idempotencyService.get(REDIS_KEY)).thenReturn(null);
        when(idempotencyService.acquireLock(REDIS_KEY)).thenReturn(LOCK_TOKEN);
    }

    private static TransferRequest request() {
        TransferRequest request = new TransferRequest();
        request.setReceiverUserId(RECEIVER_ID);
        request.setAmount(AMOUNT);
        request.setDescription("test transfer");
        return request;
    }

    private static BankTransaction transaction(
            Long id,
            String reference,
            TransactionStatus status) {

        return BankTransaction.builder()
                .id(id)
                .senderUserId(SENDER_ID)
                .receiverUserId(RECEIVER_ID)
                .amount(AMOUNT)
                .currency("INR")
                .status(status)
                .referenceId(reference)
                .idempotencyKey(IDEMPOTENCY_KEY)
                .description("test transfer")
                .build();
    }

    private static AccountTransferResponse accountResponse(String reference) {
        return AccountTransferResponse.builder()
                .transferReference(reference)
                .senderUserId(SENDER_ID)
                .receiverUserId(RECEIVER_ID)
                .senderBalance(new BigDecimal("87.50"))
                .receiverBalance(new BigDecimal("112.50"))
                .currency("INR")
                .build();
    }
}
