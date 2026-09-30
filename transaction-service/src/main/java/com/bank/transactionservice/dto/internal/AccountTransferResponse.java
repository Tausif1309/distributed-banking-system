package com.bank.transactionservice.dto.internal;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountTransferResponse {

    private String transferReference;

    private Long senderUserId;

    private Long receiverUserId;

    private BigDecimal senderBalance;

    private BigDecimal receiverBalance;

    private String currency;
}