package com.bank.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChangeRequestResponse {

    private Long id;

    private Long userId;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private String status;

    private Long reviewedBy;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}