package com.bank.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChangeRequest {

    @NotBlank(message = "Field name is required")
    private String fieldName;

    @NotBlank(message = "New value is required")
    @Size(max = 255, message = "New value cannot exceed 255 characters")
    private String newValue;
}