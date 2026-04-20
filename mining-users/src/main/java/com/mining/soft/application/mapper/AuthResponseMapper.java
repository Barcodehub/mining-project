package com.mining.soft.application.mapper;

import com.mining.soft.apifirst.model.AuthDataResponseDto;
import com.mining.soft.apifirst.model.AuthResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthResponseMapper {

    @Mapping(target = "data", source = "authResponseDto")
    AuthDataResponseDto toAuthDataResponse(AuthResponseDto authResponseDto);
}

