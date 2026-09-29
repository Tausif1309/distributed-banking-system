package com.bank.userservice.mapper;

import com.bank.userservice.dto.response.AccountResponse;
import com.bank.userservice.entity.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);
}