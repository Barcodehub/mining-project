package com.mining.soft.application.usecase;

import com.mining.soft.application.api.IAuthenticationServicePort;
import com.mining.soft.domain.model.auth.AuthResponse;
import com.mining.soft.domain.model.auth.LoginRequest;
import com.mining.soft.application.spi.IAuthenticationPort;

public class AuthenticationUseCase implements IAuthenticationServicePort {

    private final IAuthenticationPort authenticationPort;

    public AuthenticationUseCase(IAuthenticationPort authenticationPort) {
        this.authenticationPort = authenticationPort;
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        return authenticationPort.authenticate(loginRequest);
    }
}
