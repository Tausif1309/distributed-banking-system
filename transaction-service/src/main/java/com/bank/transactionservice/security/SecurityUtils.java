package com.bank.transactionservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId(
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();

        return jwt.getClaim("userId");
    }
}