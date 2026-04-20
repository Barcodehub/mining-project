package com.mining.soft.application.mapper;

import com.mining.soft.apifirst.model.PaginationMetaDto;
import com.mining.soft.apifirst.model.UserDataResponseDto;
import com.mining.soft.apifirst.model.UserResponseDto;
import com.mining.soft.apifirst.model.UsersListResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResponseMapper {


    @Mapping(target = "data", source = "userResponseDto")
    UserDataResponseDto toUserDataResponse(UserResponseDto userResponseDto);

    // Envuelver una lista de usuarios con metadata de paginación
    default UsersListResponseDto toUsersListResponse(List<UserResponseDto> users, Page<?> page) {
        PaginationMetaDto meta = toPaginationMeta(page);

        UsersListResponseDto response = new UsersListResponseDto();
        response.setData(users);
        response.setMeta(meta);
        return response;
    }

    // Extraer metadata de paginación desde un Page de Spring Data
    default PaginationMetaDto toPaginationMeta(Page<?> page) {
        if (page == null) {
            return null;
        }

        PaginationMetaDto meta = new PaginationMetaDto();
        meta.setPage(page.getNumber());
        meta.setSize(page.getSize());
        meta.setTotalElements((int) page.getTotalElements());
        meta.setTotalPages(page.getTotalPages());
        return meta;
    }
}

