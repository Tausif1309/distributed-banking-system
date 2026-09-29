package com.bank.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewChangeRequest {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;
}