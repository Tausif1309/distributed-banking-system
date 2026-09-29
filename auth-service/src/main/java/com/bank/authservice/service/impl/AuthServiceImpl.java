package com.bank.authservice.service.impl;

import com.bank.authservice.dto.request.CreateCredentialRequest;
import com.bank.authservice.dto.request.LoginRequest;
import com.bank.authservice.dto.response.LoginResponse;
import com.bank.authservice.entity.AccountStatus;
import com.bank.authservice.entity.AuthCredential;
import com.bank.authservice.exception.AccountBlockedException;
import com.bank.authservice.exception.InvalidCredentialsException;
import com.bank.authservice.repository.AuthCredentialRepository;
import com.bank.authservice.security.JwtService;
import com.bank.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthCredentialRepository authCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResponse login(LoginRequest request) {

        var authCredential = authCredentialRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid username or password"));

        if (authCredential.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountBlockedException("Account is not active");
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                authCredential.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String accessToken = jwtService.generateToken(authCredential);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(authCredential.getUserId())
                .username(authCredential.getUsername())
                .role(authCredential.getRole().name())
                .build();
    }

    @Override
    public void createCredential(CreateCredentialRequest request) {

        if (authCredentialRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        AuthCredential credential = AuthCredential.builder()
                .userId(request.getUserId())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        authCredentialRepository.save(credential);
    }
}