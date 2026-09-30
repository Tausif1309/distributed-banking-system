package com.bank.authservice.dto.request;

import com.bank.authservice.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCredentialStatusRequest {

    @NotNull(message = "Status is required")
    private AccountStatus status;
}