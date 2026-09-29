package com.bank.userservice.service;

import com.bank.userservice.dto.request.CreateUserRequest;
import com.bank.userservice.dto.request.UpdateUserRequest;
import com.bank.userservice.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long userId);

    List<UserResponse> getAllUsers();

    UserResponse updateUser(Long userId, UpdateUserRequest request);
}