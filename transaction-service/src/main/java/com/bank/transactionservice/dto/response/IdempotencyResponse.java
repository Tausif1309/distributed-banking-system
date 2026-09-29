package com.bank.transactionservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdempotencyResponse {

    private TransferResponse response;
    private int httpStatus;
}