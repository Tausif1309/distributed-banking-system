package com.bank.userservice.repository;

import com.bank.userservice.entity.ChangeRequest;
import com.bank.userservice.entity.ChangeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestRepository
        extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findByUserId(Long userId);

    List<ChangeRequest> findByStatus(ChangeRequestStatus status);
}