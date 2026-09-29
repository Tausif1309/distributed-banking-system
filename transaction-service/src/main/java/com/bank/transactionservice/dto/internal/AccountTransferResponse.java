package com.bank.transactionservice.dto.internal;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class AccountTransferResponse {

    private Long senderUserId;
    private Long receiverUserId;
    private BigDecimal senderBalance;
    private BigDecimal receiverBalance;
    private String currency;
}