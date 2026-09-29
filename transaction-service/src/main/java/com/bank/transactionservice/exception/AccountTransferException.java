package com.bank.transactionservice.exception;

public class AccountTransferException
        extends RuntimeException {

    public AccountTransferException(String message) {
        super(message);
    }

    public AccountTransferException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}