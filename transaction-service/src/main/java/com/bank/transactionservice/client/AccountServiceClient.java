package com.bank.transactionservice.client;

import com.bank.transactionservice.dto.internal.AccountTransferRequest;
import com.bank.transactionservice.dto.internal.AccountTransferResponse;
import com.bank.transactionservice.exception.AccountTransferException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestClient restClient;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${internal.service.token}")
    private String internalServiceToken;

    public AccountTransferResponse transfer(
            Long senderUserId,
            Long receiverUserId,
            BigDecimal amount) {

        AccountTransferRequest request =
                new AccountTransferRequest();

        request.setSenderUserId(senderUserId);
        request.setReceiverUserId(receiverUserId);
        request.setAmount(amount);

        try {

            return restClient
                    .post()
                    .uri(
                            userServiceUrl
                                    + "/api/v1/internal/accounts/transfer"
                    )
                    .header(
                            "X-Internal-Service-Token",
                            internalServiceToken
                    )
                    .header(
                            HttpHeaders.CONTENT_TYPE,
                            "application/json"
                    )
                    .body(request)
                    .retrieve()
                    .body(AccountTransferResponse.class);

        } catch (Exception ex) {

            throw new AccountTransferException(
                    "Account transfer failed",
                    ex
            );
        }
    }
}