package com.bank.userservice.mapper;

import com.bank.userservice.dto.response.ChangeRequestResponse;
import com.bank.userservice.entity.ChangeRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChangeRequestMapper {

    ChangeRequestResponse toResponse(ChangeRequest changeRequest);
}