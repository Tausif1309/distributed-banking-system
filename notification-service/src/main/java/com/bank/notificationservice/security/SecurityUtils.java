package com.bank.notificationservice.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "A valid access token is required"
            );
        }

        Object userIdClaim = jwt.getClaims().get("userId");
        if (!(userIdClaim instanceof Number userId) || userId.longValue() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "The access token does not contain a valid user ID"
            );
        }

        return userId.longValue();
    }
}
