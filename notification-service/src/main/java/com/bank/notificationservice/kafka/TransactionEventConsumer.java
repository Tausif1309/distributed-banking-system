
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

            Long transactionId = event.getTransactionId();
            Long receiverUserId = event.getReceiverUserId();
            String type = "MONEY_RECEIVED";

            boolean alreadyExists =
                    notificationRepository
                            .existsByTransactionIdAndUserIdAndType(
                                    transactionId,
                                    receiverUserId,
                                    type
                            );

            if (alreadyExists) {
                log.info(
                        "Duplicate notification skipped for transaction {}",
                        transactionId
                );
                return;
            }

            Notification notification =
                    Notification.builder()
                            .userId(receiverUserId)
                            .transactionId(transactionId)
                            .type(type)
                            .title("Money Received")
                            .message(
                                    "You received "
                                            + event.getCurrency()
                                            + " "
                                            + event.getAmount()
                                            + " from user "
                                            + event.getSenderUserId()
                            )
                            .read(false)
                            .createdAt(LocalDateTime.now())
                            .build();

            try {
                notificationRepository.save(notification);

                log.info(
                        "Notification created for transaction {} and user {}",
                        transactionId,
                        receiverUserId
                );

            } catch (DataIntegrityViolationException ex) {

                // A concurrent consumer may have inserted the same notification.
                // Verify that the unique constraint rejected a genuine duplicate.
                boolean duplicateNowExists =
                        notificationRepository
                                .existsByTransactionIdAndUserIdAndType(
                                        transactionId,
                                        receiverUserId,
                                        type
                                );

                if (duplicateNowExists) {
                    log.info(
                            "Concurrent duplicate notification skipped for transaction {}",
                            transactionId
                    );
                    return;
                }

                // It was another database integrity problem.
                throw ex;
            }

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
}