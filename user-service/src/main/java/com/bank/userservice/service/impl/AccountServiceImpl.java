package com.bank.userservice.service.impl;

import com.bank.userservice.dto.internal.AccountTransferRequest;
import com.bank.userservice.dto.internal.AccountTransferResponse;
import com.bank.userservice.dto.response.AccountResponse;
import com.bank.userservice.entity.*;
import com.bank.userservice.exception.ResourceNotFoundException;
import com.bank.userservice.repository.AccountRepository;
import com.bank.userservice.repository.AccountTransferRepository;
import com.bank.userservice.repository.UserRepository;
import com.bank.userservice.service.AccountService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {



    private final AccountRepository accountRepository;

    private final AccountTransferRepository accountTransferRepository;

    private final UserRepository userRepository;

    private void validateUserIsActive(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + userId
                        ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "User account is disabled or blocked"
            );
        }
    }

    @Override
    @Transactional
    public AccountTransferResponse transfer(
            AccountTransferRequest request) {

        String transferReference = request.getTransferReference();

        Long senderUserId = request.getSenderUserId();

        Long receiverUserId = request.getReceiverUserId();

        BigDecimal amount = request.getAmount();

        // 1. Validate basic request data

        if (senderUserId.equals(receiverUserId)) {
            throw new IllegalArgumentException(
                    "Sender and receiver cannot be the same"
            );
        }

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero"
            );
        }

        // 2. Check whether this transfer was already completed

        var existingTransfer =
                accountTransferRepository
                        .findByTransferReference(transferReference);

        if (existingTransfer.isPresent()) {

            return returnExistingTransfer(
                    existingTransfer.get(),
                    senderUserId,
                    receiverUserId,
                    amount
            );
        }

        /*
         * 3. Lock accounts in deterministic order.
         *
         * This reduces deadlock risk when two transfers
         * happen in opposite directions.
         */

        Long firstUserId = Math.min(
                senderUserId,
                receiverUserId
        );

        Long secondUserId = Math.max(
                senderUserId,
                receiverUserId
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

        if (firstAccount.getUserId().equals(senderUserId)) {

            senderAccount = firstAccount;
            receiverAccount = secondAccount;

        } else {

            senderAccount = secondAccount;
            receiverAccount = firstAccount;
        }

        /*
         * 4. Check again after acquiring account locks.
         *
         * Two requests with the same sender and reference
         * may have arrived concurrently.
         *
         * The second request waits for the account lock.
         * After the first transaction commits, it can see
         * the completed transfer here.
         */

        existingTransfer =
                accountTransferRepository
                        .findByTransferReference(transferReference);

        if (existingTransfer.isPresent()) {

            return returnExistingTransfer(
                    existingTransfer.get(),
                    senderUserId,
                    receiverUserId,
                    amount
            );
        }

        // Validate sender and receiver user status

        validateUserIsActive(senderUserId);
        validateUserIsActive(receiverUserId);

        // 5. Validate account states

        if (senderAccount.getStatus() != AccountStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Sender account is not active"
            );
        }

        if (receiverAccount.getStatus() != AccountStatus.ACTIVE) {

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

        if (senderAccount.getBalance().compareTo(amount) < 0) {

            throw new IllegalStateException(
                    "Insufficient balance"
            );
        }

        // 6. Create transfer record

        LocalDateTime now = LocalDateTime.now();

        AccountTransfer transfer = AccountTransfer.builder()
                .transferReference(transferReference)
                .senderUserId(senderUserId)
                .receiverUserId(receiverUserId)
                .amount(amount)
                .currency(senderAccount.getCurrency())
                .status(AccountTransferStatus.PENDING)
                .createdAt(now)
                .build();

        accountTransferRepository.save(transfer);

        // 7. Update both balances

        senderAccount.setBalance(
                senderAccount.getBalance().subtract(amount)
        );

        receiverAccount.setBalance(
                receiverAccount.getBalance().add(amount)
        );

        accountRepository.save(senderAccount);

        accountRepository.save(receiverAccount);

        // 8. Mark transfer completed

        transfer.setSenderBalanceAfter(
                senderAccount.getBalance()
        );

        transfer.setReceiverBalanceAfter(
                receiverAccount.getBalance()
        );

        transfer.setStatus(AccountTransferStatus.COMPLETED);

        transfer.setCompletedAt(LocalDateTime.now());

        accountTransferRepository.save(transfer);

        // 9. Return result

        return AccountTransferResponse.builder()
                .transferReference(transferReference)
                .senderUserId(senderUserId)
                .receiverUserId(receiverUserId)
                .senderBalance(senderAccount.getBalance())
                .receiverBalance(receiverAccount.getBalance())
                .currency(senderAccount.getCurrency())
                .build();
    }

    private AccountTransferResponse returnExistingTransfer(
            AccountTransfer transfer,
            Long senderUserId,
            Long receiverUserId,
            BigDecimal amount) {

        /*
         * Same reference must represent the same operation.
         * Never return an old result for a different request.
         */

        boolean sameRequest =
                transfer.getSenderUserId().equals(senderUserId)
                        && transfer.getReceiverUserId().equals(receiverUserId)
                        && transfer.getAmount().compareTo(amount) == 0;

        if (!sameRequest) {

            throw new IllegalArgumentException(
                    "Transfer reference was already used with different details"
            );
        }

        if (transfer.getStatus() != AccountTransferStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Transfer is not completed"
            );
        }

        return AccountTransferResponse.builder()
                .transferReference(transfer.getTransferReference())
                .senderUserId(transfer.getSenderUserId())
                .receiverUserId(transfer.getReceiverUserId())
                .senderBalance(transfer.getSenderBalanceAfter())
                .receiverBalance(transfer.getReceiverBalanceAfter())
                .currency(transfer.getCurrency())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByUserId(Long userId) {

        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Account not found for user " + userId
                        ));

        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}