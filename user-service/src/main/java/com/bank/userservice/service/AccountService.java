package com.bank.userservice.service;

import com.bank.userservice.dto.internal.AccountTransferRequest;
import com.bank.userservice.dto.internal.AccountTransferResponse;

public interface AccountService {

    AccountTransferResponse transfer(
            AccountTransferRequest request
    );
}