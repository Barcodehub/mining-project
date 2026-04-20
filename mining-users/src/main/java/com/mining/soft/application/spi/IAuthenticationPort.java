package com.mining.soft.application.spi;

import com.mining.soft.domain.model.auth.AuthResponse;
import com.mining.soft.domain.model.auth.LoginRequest;

public interface IAuthenticationPort {

    AuthResponse authenticate(LoginRequest loginRequest);

}
