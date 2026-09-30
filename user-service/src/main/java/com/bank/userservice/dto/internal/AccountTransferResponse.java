package com.bank.userservice.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class AccountTransferResponse {

    private String transferReference;

    private Long senderUserId;

    private Long receiverUserId;

    private BigDecimal senderBalance;

    private BigDecimal receiverBalance;

    private String currency;
}