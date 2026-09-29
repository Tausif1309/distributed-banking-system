package com.bank.authservice.service;

import com.bank.authservice.dto.request.CreateCredentialRequest;
import com.bank.authservice.dto.request.LoginRequest;
import com.bank.authservice.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void createCredential(CreateCredentialRequest request);
}