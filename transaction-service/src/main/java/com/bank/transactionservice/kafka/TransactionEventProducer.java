package com.bank.transactionservice.kafka;

import com.bank.transactionservice.dto.event.TransactionCompletedEvent;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionEventProducer {

    private static final String TOPIC = "transaction-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;


    public void publishTransactionCompleted(TransactionCompletedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(TOPIC, event.getReferenceId(), json)
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish transaction event", e);
        }
    }
}