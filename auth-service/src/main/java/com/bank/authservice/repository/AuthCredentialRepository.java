package com.bank.authservice.repository;

import com.bank.authservice.entity.AuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthCredentialRepository extends JpaRepository<AuthCredential, Long> {

    Optional<AuthCredential> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<AuthCredential> findByUserId(Long userId);
}