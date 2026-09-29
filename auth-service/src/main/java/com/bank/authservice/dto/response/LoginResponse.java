package com.bank.authservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;

    private String tokenType;

    private Long userId;

    private String username;

    private String role;
}