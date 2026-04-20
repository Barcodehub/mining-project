package com.mining.soft.infrastructure.input;


import com.mining.soft.apifirst.api.UsersApi;
import com.mining.soft.apifirst.model.UserDataResponseDto;
import com.mining.soft.apifirst.model.UserRequestDto;
import com.mining.soft.apifirst.model.UserResponseDto;
import com.mining.soft.apifirst.model.UsersListResponseDto;
import com.mining.soft.application.api.IUserServicePort;
import com.mining.soft.application.mapper.IUserMapper;
import com.mining.soft.application.mapper.ResponseMapper;
import com.mining.soft.domain.enums.RoleEnum;
import com.mining.soft.domain.model.UserModel;
import com.mining.soft.infrastructure.security.annotations.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController implements UsersApi {

    private final IUserServicePort userHandler;
    private final ResponseMapper responseMapper;
    private final IUserMapper userMapper;


    @RequireRole({RoleEnum.ADMINISTRADOR, RoleEnum.EMPLEADO, RoleEnum.PROPIETARIO})
    @Override
    public ResponseEntity<UserDataResponseDto> createUser(UserRequestDto userRequestDto) {

        UserModel userModel = userMapper.toDomain(userRequestDto);
        UserModel response = userHandler.createUser(userModel);
        UserDataResponseDto userResponseDto = userMapper.toResponseDataDto(response);

        return new ResponseEntity<>(userResponseDto, HttpStatus.CREATED);
    }

    @RequireRole({RoleEnum.ADMINISTRADOR})
    @Override
    public ResponseEntity<UsersListResponseDto> getAllUsers(Integer page, Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);


        Page<UserModel> userPage = userHandler.getAllUsers(pageable);

        List<UserResponseDto> userResponseDtos = userPage.getContent().stream()
                .map(userMapper::toResponseDto)
                .toList();

        UsersListResponseDto response = responseMapper.toUsersListResponse(userResponseDtos, userPage);

        return ResponseEntity.ok(response);
    }

    @RequireRole({RoleEnum.ADMINISTRADOR, RoleEnum.EMPLEADO, RoleEnum.PROPIETARIO, RoleEnum.CLIENTE})
    @Override
    public ResponseEntity<UserDataResponseDto> getUserById(Long id) {
        UserModel response = userHandler.getUserById(id);

        UserResponseDto userResponseDto = userMapper.toResponseDto(response);
        UserDataResponseDto responseDto =  responseMapper.toUserDataResponse(userResponseDto);
        return ResponseEntity.ok(responseDto);
    }

    @Override
    public ResponseEntity<UserDataResponseDto> publicRegisterUser(UserRequestDto userRequestDto) {

        UserModel userModel = userMapper.toDomain(userRequestDto);
        UserModel response = userHandler.createUser(userModel);
        UserDataResponseDto userResponseDto = userMapper.toResponseDataDto(response);

        return new ResponseEntity<>(userResponseDto, HttpStatus.CREATED);
    }

}
