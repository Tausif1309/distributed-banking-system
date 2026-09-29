package com.bank.userservice.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CreateCredentialRequest {

    private Long userId;
    private String username;
    private String password;
    private String role;
}