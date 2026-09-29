package com.bank.userservice.service.impl;

import com.bank.userservice.dto.request.CreateChangeRequest;
import com.bank.userservice.dto.request.ReviewChangeRequest;
import com.bank.userservice.dto.response.ChangeRequestResponse;
import com.bank.userservice.entity.ChangeRequest;
import com.bank.userservice.entity.ChangeRequestStatus;
import com.bank.userservice.entity.User;
import com.bank.userservice.exception.ResourceAlreadyExistsException;
import com.bank.userservice.exception.ResourceNotFoundException;
import com.bank.userservice.mapper.ChangeRequestMapper;
import com.bank.userservice.repository.ChangeRequestRepository;
import com.bank.userservice.repository.UserRepository;
import com.bank.userservice.service.ChangeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeRequestServiceImpl
        implements ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;
    private final ChangeRequestMapper changeRequestMapper;

    @Override
    @Transactional
    public ChangeRequestResponse createRequest(
            Long userId,
            CreateChangeRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        String fieldName = request.getFieldName().trim();

        String oldValue = getCurrentValue(
                user,
                fieldName
        );

        ChangeRequest changeRequest =
                ChangeRequest.builder()
                        .userId(userId)
                        .fieldName(fieldName)
                        .oldValue(oldValue)
                        .newValue(request.getNewValue())
                        .status(ChangeRequestStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

        return changeRequestMapper.toResponse(
                changeRequestRepository.save(changeRequest)
        );
    }

    private String getCurrentValue(
            User user,
            String fieldName) {

        return switch (fieldName) {

            case "fullName" ->
                    user.getFullName();

            case "email" ->
                    user.getEmail();

            case "phone" ->
                    user.getPhone();

            case "address" ->
                    user.getAddress();

            default ->
                    throw new IllegalArgumentException(
                            "Field cannot be modified: " + fieldName
                    );
        };
    }

    private void applyChange(
            User user,
            String fieldName,
            String newValue) {

        switch (fieldName) {

            case "fullName":
                user.setFullName(newValue);
                break;

            case "email":

                if (userRepository.existsByEmail(newValue)
                        && !newValue.equals(user.getEmail())) {

                    throw new ResourceAlreadyExistsException(
                            "Email is already in use"
                    );
                }

                user.setEmail(newValue);
                break;

            case "phone":

                if (userRepository.existsByPhone(newValue)
                        && !newValue.equals(user.getPhone())) {

                    throw new ResourceAlreadyExistsException(
                            "Phone number is already in use"
                    );
                }

                user.setPhone(newValue);
                break;

            case "address":
                user.setAddress(newValue);
                break;

            default:
                throw new IllegalArgumentException(
                        "Field cannot be modified: " + fieldName
                );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> getMyRequests(
            Long userId) {

        return changeRequestRepository
                .findByUserId(userId)
                .stream()
                .map(changeRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChangeRequestResponse> getPendingRequests() {

        return changeRequestRepository
                .findByStatus(ChangeRequestStatus.PENDING)
                .stream()
                .map(changeRequestMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChangeRequestResponse reviewRequest(
            Long requestId,
            Long adminId,
            ReviewChangeRequest request) {

        ChangeRequest changeRequest =
                changeRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Change request not found"
                                ));

        // Prevent reviewing the same request twice
        if (changeRequest.getStatus() != ChangeRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Change request has already been reviewed"
            );
        }

        // If rejected, don't modify the user
        if (!request.getApproved()) {

            changeRequest.setStatus(
                    ChangeRequestStatus.REJECTED
            );

            changeRequest.setReviewedBy(adminId);
            changeRequest.setReviewedAt(LocalDateTime.now());

            return changeRequestMapper.toResponse(
                    changeRequestRepository.save(changeRequest)
            );
        }

        // Approved → update the user
        User user = userRepository.findById(
                changeRequest.getUserId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "User associated with change request not found"
                ));

        applyChange(
                user,
                changeRequest.getFieldName(),
                changeRequest.getNewValue()
        );

        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        changeRequest.setStatus(
                ChangeRequestStatus.APPROVED
        );

        changeRequest.setReviewedBy(adminId);
        changeRequest.setReviewedAt(LocalDateTime.now());

        return changeRequestMapper.toResponse(
                changeRequestRepository.save(changeRequest)
        );
    }
}