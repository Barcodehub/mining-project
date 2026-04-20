package com.mining.soft.application.api;

import com.mining.soft.domain.model.auth.AuthResponse;
import com.mining.soft.domain.model.auth.LoginRequest;
import org.springframework.stereotype.Service;

@Service
public interface IAuthenticationServicePort {

    AuthResponse login(LoginRequest loginRequest);

}
