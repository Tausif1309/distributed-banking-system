package com.bank.userservice.service.impl;

import com.bank.userservice.dto.internal.AccountTransferRequest;
import com.bank.userservice.dto.internal.AccountTransferResponse;
import com.bank.userservice.entity.Account;
import com.bank.userservice.entity.AccountStatus;
import com.bank.userservice.exception.ResourceNotFoundException;
import com.bank.userservice.repository.AccountRepository;
import com.bank.userservice.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl
        implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public AccountTransferResponse transfer(
            AccountTransferRequest request) {

        if (request.getSenderUserId()
                .equals(request.getReceiverUserId())) {

            throw new IllegalArgumentException(
                    "Sender and receiver cannot be the same"
            );
        }

        BigDecimal amount = request.getAmount();

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero"
            );
        }

        /*
         * IMPORTANT:
         * Always acquire the two account locks
         * in a deterministic order.
         *
         * This reduces deadlock risk when two
         * transfers happen in opposite directions.
         */
        Long firstUserId =
                Math.min(
                        request.getSenderUserId(),
                        request.getReceiverUserId()
                );

        Long secondUserId =
                Math.max(
                        request.getSenderUserId(),
                        request.getReceiverUserId()
                );

        Account firstAccount =
                accountRepository.findByUserIdForUpdate(firstUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found for user "
                                                + firstUserId
                                ));

        Account secondAccount =
                accountRepository.findByUserIdForUpdate(secondUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found for user "
                                                + secondUserId
                                ));

        Account senderAccount;

        Account receiverAccount;

        if (firstAccount.getUserId()
                .equals(request.getSenderUserId())) {

            senderAccount = firstAccount;
            receiverAccount = secondAccount;

        } else {

            senderAccount = secondAccount;
            receiverAccount = firstAccount;
        }

        if (senderAccount.getStatus()
                != AccountStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Sender account is not active"
            );
        }

        if (receiverAccount.getStatus()
                != AccountStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Receiver account is not active"
            );
        }

        if (!senderAccount.getCurrency()
                .equals(receiverAccount.getCurrency())) {

            throw new IllegalStateException(
                    "Currency mismatch"
            );
        }

        if (senderAccount.getBalance()
                .compareTo(amount) < 0) {

            throw new IllegalStateException(
                    "Insufficient balance"
            );
        }

        senderAccount.setBalance(
                senderAccount.getBalance()
                        .subtract(amount)
        );

        receiverAccount.setBalance(
                receiverAccount.getBalance()
                        .add(amount)
        );

        accountRepository.save(senderAccount);
        accountRepository.save(receiverAccount);

        return AccountTransferResponse.builder()
                .senderUserId(
                        request.getSenderUserId()
                )
                .receiverUserId(
                        request.getReceiverUserId()
                )
                .senderBalance(
                        senderAccount.getBalance()
                )
                .receiverBalance(
                        receiverAccount.getBalance()
                )
                .currency(
                        senderAccount.getCurrency()
                )
                .build();
    }
}