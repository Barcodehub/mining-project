package com.mining.soft.application.mapper;

import com.mining.soft.apifirst.model.UserDataResponseDto;
import com.mining.soft.apifirst.model.UserRequestDto;
import com.mining.soft.apifirst.model.UserResponseDto;
import com.mining.soft.domain.model.RoleModel;
import com.mining.soft.domain.model.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;


@Mapper(componentModel = "spring", uses = {IRoleMapper.class})
public interface IUserMapper {

    @Mapping(source = "roleId", target = "role", qualifiedByName = "roleIdToRoleModel")
    UserModel toDomain(UserRequestDto userRequestDto);

    UserResponseDto toResponseDto(UserModel userModel);

    @Named("roleIdToRoleModel")
    default RoleModel roleIdToRoleModel(Long roleId) {
        if (roleId == null) {
            return null;
        }
        RoleModel roleModel = new RoleModel();
        roleModel.setId(roleId);
        return roleModel;
    }

    @Mapping(source = ".", target = "data")
    UserDataResponseDto toResponseDataDto(UserModel response);
}
