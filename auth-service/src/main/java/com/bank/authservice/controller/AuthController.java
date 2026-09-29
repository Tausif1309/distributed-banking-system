package com.bank.authservice.controller;

import com.bank.authservice.dto.request.CreateCredentialRequest;
import com.bank.authservice.dto.request.LoginRequest;
import com.bank.authservice.dto.response.LoginResponse;
import com.bank.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/credentials")
    public ResponseEntity<Void> createCredential(
            @Valid @RequestBody CreateCredentialRequest request) {

        authService.createCredential(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}