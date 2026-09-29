package com.bank.userservice.service.impl;

import com.bank.userservice.client.AuthServiceClient;
import com.bank.userservice.dto.request.CreateUserRequest;
import com.bank.userservice.dto.request.UpdateUserRequest;
import com.bank.userservice.dto.response.UserResponse;
import com.bank.userservice.entity.Account;
import com.bank.userservice.entity.AccountStatus;
import com.bank.userservice.entity.User;
import com.bank.userservice.entity.UserStatus;
import com.bank.userservice.exception.ResourceAlreadyExistsException;
import com.bank.userservice.exception.ResourceNotFoundException;
import com.bank.userservice.mapper.AccountMapper;
import com.bank.userservice.mapper.UserMapper;
import com.bank.userservice.repository.AccountRepository;
import com.bank.userservice.repository.UserRepository;
import com.bank.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;

    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email is already registered");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new ResourceAlreadyExistsException("Phone number is already registered");
        }

        User user = userMapper.toEntity(request);

        user.setStatus(UserStatus.ACTIVE);

        LocalDateTime now = LocalDateTime.now();

        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        BigDecimal initialBalance = request.getInitialBalance();

        if (initialBalance == null) {
            initialBalance = BigDecimal.ZERO;
        }

        Account account = Account.builder()
                .userId(savedUser.getId())
                .balance(initialBalance)
                .currency("INR")
                .status(AccountStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        accountRepository.save(account);

        authServiceClient.createUserCredential(
                savedUser.getId(),
                request.getUsername(),
                request.getPassword()
        );

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {

        List<User> users = userRepository.findAll();

        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getEmail() != null &&
                !request.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {

            throw new ResourceAlreadyExistsException("Email already exists");
        }

        if (request.getPhone() != null &&
                !request.getPhone().equals(user.getPhone()) &&
                userRepository.existsByPhone(request.getPhone())) {

            throw new ResourceAlreadyExistsException("Phone number already exists");
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);

        return userMapper.toResponse(updatedUser);
    }
}