package com.mining.soft.application.mapper;

import com.mining.soft.apifirst.model.RoleResponseDto;
import com.mining.soft.domain.model.RoleModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IRoleMapper {

    RoleResponseDto toResponseDto(RoleModel roleModel);

    RoleModel toModel(RoleResponseDto roleResponseDto);
}

