package com.mining.soft.infrastructure.out.jpa.mapper;

import com.mining.soft.domain.model.RoleModel;
import com.mining.soft.infrastructure.out.jpa.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IRoleEntityMapper {

    RoleModel toDomain(RoleEntity roleEntity);

    RoleEntity toEntity(RoleModel roleModel);

}
