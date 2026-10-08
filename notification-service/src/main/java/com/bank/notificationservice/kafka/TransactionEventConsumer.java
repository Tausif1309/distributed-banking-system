
package com.bank.notificationservice.kafka;

import com.bank.notificationservice.dto.event.TransactionCompletedEvent;
import com.bank.notificationservice.entity.Notification;
import com.bank.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "notification-service"
    )
    public void consumeTransactionCompleted(String message) {

        try {
            TransactionCompletedEvent event =
                    objectMapper.readValue(
                            message,
                            TransactionCompletedEvent.class
                    );

            // The transaction producer emits this event only after storing
            // a successful transfer. Other event types must not notify users.
            if (!"TRANSACTION_COMPLETED".equals(event.getEventType())) {
                log.info(
                        "Ignoring non-completed transaction event {}",
                        event.getEventType()
                );
                return;
            }

            if (event.getTransactionId() == null
                    || event.getReceiverUserId() == null
                    || event.getSenderUserId() == null
                    || event.getAmount() == null
                    || event.getCurrency() == null
                    || event.getReferenceId() == null) {

                throw new IllegalArgumentException(
                        "Transaction event contains missing required fields"
                );
            }

            if (event.getTransactionId() <= 0
                    || event.getReceiverUserId() <= 0
                    || event.getSenderUserId() <= 0
                    || event.getAmount().signum() <= 0
                    || event.getCurrency().isBlank()
                    || event.getReferenceId().isBlank()) {
                throw new IllegalArgumentException(
                        "Transaction event contains invalid required fields"
                );
            }

            Long transactionId = event.getTransactionId();
            Long receiverUserId = event.getReceiverUserId();

            createNotification(
                    transactionId,
                    receiverUserId,
                    "MONEY_RECEIVED",
                    "Money Received",
                    "You received "
                            + event.getCurrency()
                            + " "
                            + event.getAmount()
                            + " from user "
                            + event.getSenderUserId()
            );

            createNotification(
                    transactionId,
                    event.getSenderUserId(),
                    "MONEY_SENT",
                    "Money Sent",
                    "You sent "
                            + event.getCurrency()
                            + " "
                            + event.getAmount()
                            + " to user "
                            + receiverUserId
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to process transaction event",
                    ex
            );

            throw new RuntimeException(
                    "Failed to process Kafka transaction event",
                    ex
            );
        }
    }

    private void createNotification(
            Long transactionId,
            Long userId,
            String type,
            String title,
            String message) {

        if (notificationRepository
                .existsByTransactionIdAndUserIdAndType(
                        transactionId,
                        userId,
                        type
                )) {
            log.info(
                    "Duplicate {} notification skipped for transaction {} and user {}",
                    type,
                    transactionId,
                    userId
            );
            return;
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .transactionId(transactionId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException ex) {
            // The database unique constraint is the final guard if two
            // consumers race after both pass the existence check.
            if (notificationRepository
                    .existsByTransactionIdAndUserIdAndType(
                            transactionId,
                            userId,
                            type
                    )) {
                log.info(
                        "Concurrent duplicate {} notification skipped for transaction {}",
                        type,
                        transactionId
                );
                return;
            }

            throw ex;
        }

        log.info(
                "{} notification created for transaction {} and user {}",
                type,
                transactionId,
                userId
        );
    }
}
