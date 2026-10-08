package com.bank.notificationservice.kafka;

import com.bank.notificationservice.dto.event.TransactionCompletedEvent;
import com.bank.notificationservice.entity.Notification;
import com.bank.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionEventConsumerTest {

    private final NotificationRepository repository =
            mock(NotificationRepository.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final TransactionEventConsumer consumer =
            new TransactionEventConsumer(repository, objectMapper);

    @Test
    void createsSentAndReceivedNotificationsForCompletedTransfer() throws Exception {
        when(objectMapper.readValue(anyString(), eq(TransactionCompletedEvent.class)))
                .thenReturn(completedEvent("TRANSACTION_COMPLETED"));

        consumer.consumeTransactionCompleted("event-json");

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());

        var notifications = captor.getAllValues();
        assertEquals("MONEY_RECEIVED", notifications.get(0).getType());
        assertEquals(22L, notifications.get(0).getUserId());
        assertEquals("MONEY_SENT", notifications.get(1).getType());
        assertEquals(11L, notifications.get(1).getUserId());
    }

    @Test
    void skipsAlreadyStoredNotificationsWhenEventIsDeliveredAgain() throws Exception {
        when(objectMapper.readValue(anyString(), eq(TransactionCompletedEvent.class)))
                .thenReturn(completedEvent("TRANSACTION_COMPLETED"));
        when(repository.existsByTransactionIdAndUserIdAndType(
                700L, 22L, "MONEY_RECEIVED"
        )).thenReturn(true);
        when(repository.existsByTransactionIdAndUserIdAndType(
                700L, 11L, "MONEY_SENT"
        )).thenReturn(true);

        consumer.consumeTransactionCompleted("event-json");

        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void ignoresNonCompletedEvents() throws Exception {
        when(objectMapper.readValue(anyString(), eq(TransactionCompletedEvent.class)))
                .thenReturn(completedEvent("TRANSFER_PENDING"));

        consumer.consumeTransactionCompleted("event-json");

        verifyNoInteractions(repository);
    }

    private TransactionCompletedEvent completedEvent(String eventType) {
        return TransactionCompletedEvent.builder()
                .transactionId(700L)
                .referenceId("TXN-test-reference")
                .senderUserId(11L)
                .receiverUserId(22L)
                .amount(new BigDecimal("5.00"))
                .currency("INR")
                .description("Test transfer")
                .eventType(eventType)
                .build();
    }
}
