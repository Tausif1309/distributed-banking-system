package com.bank.userservice.service;

import com.bank.userservice.dto.request.CreateChangeRequest;
import com.bank.userservice.dto.request.ReviewChangeRequest;
import com.bank.userservice.dto.response.ChangeRequestResponse;

import java.util.List;

public interface ChangeRequestService {

    ChangeRequestResponse createRequest(
            Long userId,
            CreateChangeRequest request
    );

    List<ChangeRequestResponse> getMyRequests(
            Long userId
    );

    List<ChangeRequestResponse> getPendingRequests();

    ChangeRequestResponse reviewRequest(
            Long requestId,
            Long adminId,
            ReviewChangeRequest request
    );
}