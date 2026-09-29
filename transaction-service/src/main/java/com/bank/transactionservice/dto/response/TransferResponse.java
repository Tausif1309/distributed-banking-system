package com.bank.transactionservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransferResponse implements java.io.Serializable {

    private Long transactionId;

    private String referenceId;

    private Long senderUserId;

    private Long receiverUserId;

    private BigDecimal amount;

    private String currency;

    private String status;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}