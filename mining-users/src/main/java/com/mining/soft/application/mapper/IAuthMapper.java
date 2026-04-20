package com.mining.soft.application.mapper;

import com.mining.soft.apifirst.model.AuthResponseDto;
import com.mining.soft.apifirst.model.LoginRequestDto;
import com.mining.soft.domain.model.auth.AuthResponse;
import com.mining.soft.domain.model.auth.LoginRequest;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring", uses = {IRoleMapper.class})
public interface IAuthMapper {

    LoginRequest toDto(LoginRequestDto userRequestDto);

    AuthResponseDto toResponseDto(AuthResponse userModel);
}
