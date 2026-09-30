package com.bank.userservice.dto.internal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountTransferRequest {

    @NotBlank(message = "Transfer reference is required")
    @Size(max = 100, message = "Transfer reference cannot exceed 100 characters")
    private String transferReference;

    @NotNull(message = "Sender user ID is required")
    @Positive(message = "Sender user ID must be positive")
    private Long senderUserId;

    @NotNull(message = "Receiver user ID is required")
    @Positive(message = "Receiver user ID must be positive")
    private Long receiverUserId;

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Transfer amount must be at least 0.01"
    )
    @Digits(
            integer = 17,
            fraction = 2,
            message = "Amount must have at most 17 integer digits and 2 decimal places"
    )
    private BigDecimal amount;
}