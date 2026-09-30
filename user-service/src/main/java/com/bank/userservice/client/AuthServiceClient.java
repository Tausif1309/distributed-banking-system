package com.bank.userservice.client;

import com.bank.userservice.dto.internal.CreateCredentialRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestClient restClient;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    public void createUserCredential(
            Long userId,
            String username,
            String password) {

        CreateCredentialRequest request =
                CreateCredentialRequest.builder()
                        .userId(userId)
                        .username(username)
                        .password(password)
                        .role("USER")
                        .build();

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new IllegalStateException(
                    "No current HTTP request available"
            );
        }

        String authorizationHeader =
                attributes.getRequest()
                        .getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null ||
                authorizationHeader.isBlank()) {

            throw new IllegalStateException(
                    "Authorization header is missing"
            );
        }

        restClient
                .post()
                .uri(authServiceUrl + "/api/v1/auth/credentials")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        authorizationHeader
                )
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public void updateCredentialStatus(
            Long userId,
            String status) {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new IllegalStateException(
                    "No current HTTP request available"
            );
        }

        String authorizationHeader =
                attributes.getRequest()
                        .getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null ||
                authorizationHeader.isBlank()) {

            throw new IllegalStateException(
                    "Authorization header is missing"
            );
        }

        restClient
                .put()
                .uri(authServiceUrl +
                        "/api/v1/auth/credentials/" + userId + "/status")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .body(java.util.Map.of("status", status))
                .retrieve()
                .toBodilessEntity();
    }
}