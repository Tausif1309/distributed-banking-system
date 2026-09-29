package com.bank.transactionservice.dto.internal;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountTransferRequest {

    private Long senderUserId;

    private Long receiverUserId;

    private BigDecimal amount;
}