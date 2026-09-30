package com.bank.userservice.repository;

import com.bank.userservice.entity.AccountTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountTransferRepository
        extends JpaRepository<AccountTransfer, Long> {

    Optional<AccountTransfer> findByTransferReference(
            String transferReference
    );
}