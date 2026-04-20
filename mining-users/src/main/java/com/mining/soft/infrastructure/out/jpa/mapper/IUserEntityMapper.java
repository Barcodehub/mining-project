package com.mining.soft.infrastructure.out.jpa.mapper;

import com.mining.soft.domain.model.UserModel;
import com.mining.soft.infrastructure.out.jpa.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring", uses = {IRoleEntityMapper.class})
public interface IUserEntityMapper {

    @Mapping(target = "createdBy", expression = "java(mapUserIdSimple(entity.getCreatedBy()))")
    UserModel toDomain(UserEntity entity);

    UserEntity toEntity(UserModel userModel);


    default UserModel mapUserIdSimple(UserEntity userEntity) {
        if (userEntity == null) return null;
        UserModel u = new UserModel();
        u.setId(userEntity.getId());
        return u;
    }
}
