package com.bank.transactionservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TransferRequest {

    @NotNull(message = "Receiver user ID is required")
    private Long receiverUserId;

    @NotNull(message = "Amount is required")
    @DecimalMin(
            value = "0.01",
            message = "Transfer amount must be greater than zero"
    )
    private BigDecimal amount;

    @Size(
            max = 255,
            message = "Description cannot exceed 255 characters"
    )
    private String description;
}