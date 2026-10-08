package com.bank.transactionservice.exception;

import org.springframework.web.client.RestClientResponseException;

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

    /**
     * A 4xx response means User Service rejected the request before
     * applying the account movement. Network errors and 5xx responses
     * remain uncertain because User Service may already have committed.
     */
    public boolean isDefinitiveRejection() {
        Throwable cause = getCause();

        while (cause != null) {
            if (cause instanceof RestClientResponseException responseException) {
                return responseException.getStatusCode().is4xxClientError();
            }

            cause = cause.getCause();
        }

        return false;
    }
}
