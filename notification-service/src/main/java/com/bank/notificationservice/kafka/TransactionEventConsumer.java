package com.bank.notificationservice.kafka;

import com.bank.notificationservice.dto.event.TransactionCompletedEvent;
import com.bank.notificationservice.entity.Notification;
import com.bank.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
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

            System.out.println(
                    "Received transaction event: "
                            + event.getReferenceId()
            );

            Long receiverUserId =
                    event.getReceiverUserId();

            String type = "MONEY_RECEIVED";

            boolean alreadyExists =
                    notificationRepository
                            .existsByTransactionIdAndUserIdAndType(
                                    event.getTransactionId(),
                                    receiverUserId,
                                    type
                            );

            if (alreadyExists) {

                System.out.println(
                        "Notification already exists for transaction: "
                                + event.getTransactionId()
                );

                return;
            }

            Notification notification =
                    Notification.builder()
                            .userId(receiverUserId)
                            .transactionId(event.getTransactionId())
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

            notificationRepository.save(notification);

            System.out.println(
                    "Notification saved for user: "
                            + receiverUserId
            );

        } catch (Exception e) {

            System.err.println(
                    "Failed to process transaction event: "
                            + e.getMessage()
            );

            throw new RuntimeException(
                    "Failed to process Kafka transaction event",
                    e
            );
        }
    }
}