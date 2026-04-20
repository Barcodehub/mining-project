package com.mining.soft.infrastructure.input;

import com.mining.soft.apifirst.api.AuthApi;
import com.mining.soft.apifirst.model.AuthDataResponseDto;
import com.mining.soft.apifirst.model.AuthResponseDto;
import com.mining.soft.apifirst.model.LoginRequestDto;
import com.mining.soft.application.api.IAuthenticationServicePort;
import com.mining.soft.application.mapper.AuthResponseMapper;
import com.mining.soft.application.mapper.IAuthMapper;
import com.mining.soft.domain.model.auth.AuthResponse;
import com.mining.soft.domain.model.auth.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthenticationController implements AuthApi {

    private final IAuthenticationServicePort authenticationHandler;
    private final IAuthMapper authMapper;
    private final AuthResponseMapper authResponseMapper;

    @Override
    public ResponseEntity<AuthDataResponseDto> login(LoginRequestDto loginRequestDto) {


        LoginRequest loginRequest = authMapper.toDto(loginRequestDto);
        AuthResponse authResponse = authenticationHandler.login(loginRequest);
        AuthResponseDto authResponseDto = authMapper.toResponseDto(authResponse);

        AuthDataResponseDto responseDto = authResponseMapper.toAuthDataResponse(authResponseDto);

        return ResponseEntity.ok(responseDto);
    }
}
