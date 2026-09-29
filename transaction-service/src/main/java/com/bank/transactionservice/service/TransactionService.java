package com.bank.transactionservice.service;

import com.bank.transactionservice.dto.request.TransferRequest;
import com.bank.transactionservice.dto.response.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransactionService {

    TransferResponse transfer(
            Long senderUserId,
            TransferRequest request,
            String idempotencyKey);

    List<TransferResponse> getMyTransactions(Long userId);

    Page<TransferResponse> getAllTransactions(
            Pageable pageable);
}