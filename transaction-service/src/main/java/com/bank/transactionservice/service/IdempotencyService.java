package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.response.TransferResponse;

public interface IdempotencyService {

    TransferResponse get(String key);

    String acquireLock(String key);

    void releaseLock(String key, String lockToken);

    void save(String key, TransferResponse response);
}