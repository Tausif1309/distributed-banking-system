package com.bank.notificationservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCompletedEvent {

    private Long transactionId;

    private String referenceId;

    private Long senderUserId;

    private Long receiverUserId;

    private BigDecimal amount;

    private String currency;

    private String description;

    private String eventType;
}