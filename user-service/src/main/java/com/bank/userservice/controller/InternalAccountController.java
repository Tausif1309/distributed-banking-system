package com.bank.userservice.controller;

import com.bank.userservice.dto.internal.AccountTransferRequest;
import com.bank.userservice.dto.internal.AccountTransferResponse;
import com.bank.userservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/accounts")
@RequiredArgsConstructor
public class InternalAccountController {

    private final AccountService accountService;

    @PostMapping("/transfer")
    public ResponseEntity<AccountTransferResponse> transfer(
            @Valid @RequestBody AccountTransferRequest request) {

        return ResponseEntity.ok(
                accountService.transfer(request)
        );
    }
}