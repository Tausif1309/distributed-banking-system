package com.bank.transactionservice.controller;

import com.bank.transactionservice.dto.request.TransferRequest;
import com.bank.transactionservice.dto.response.TransferResponse;
import com.bank.transactionservice.security.SecurityUtils;
import com.bank.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        Long senderUserId =
                SecurityUtils.getCurrentUserId(authentication);

        TransferResponse response =
                transactionService.transfer(
                        senderUserId,
                        request,idempotencyKey
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TransferResponse>> getMyTransactions(
            Authentication authentication) {

        Long userId = SecurityUtils.getCurrentUserId(authentication);

        List<TransferResponse> transactions =
                transactionService.getMyTransactions(userId);

        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TransferResponse>> getAllTransactions(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        Page<TransferResponse> transactions =
                transactionService.getAllTransactions(pageable);

        return ResponseEntity.ok(transactions);
    }
}