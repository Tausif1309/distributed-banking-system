package com.bank.userservice.controller;

import com.bank.userservice.dto.response.AccountResponse;
import com.bank.userservice.security.SecurityUtils;
import com.bank.userservice.service.AccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AccountResponse> getMyAccount(
            Authentication authentication) {

        Long userId =
                SecurityUtils.getCurrentUserId(authentication);

        AccountResponse response =
                accountService.getAccountByUserId(userId);

        return ResponseEntity.ok(response);
    }
}