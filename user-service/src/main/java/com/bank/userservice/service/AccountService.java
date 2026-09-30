package com.bank.userservice.service;

import com.bank.userservice.dto.internal.AccountTransferRequest;
import com.bank.userservice.dto.internal.AccountTransferResponse;
import com.bank.userservice.dto.response.AccountResponse;

public interface AccountService {

    AccountTransferResponse transfer(
            AccountTransferRequest request
    );

    AccountResponse getAccountByUserId(Long userId);
}