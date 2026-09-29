package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.response.TransferResponse;

public interface IdempotencyService {

    TransferResponse get(String key);

    boolean acquireLock(String key);

    void save(String key, TransferResponse response);

    void delete(String key);
}