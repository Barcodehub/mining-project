package com.mining.soft.infrastructure.configuration;


import com.mining.soft.application.api.IAuthenticationServicePort;
import com.mining.soft.application.api.IUserServicePort;
import com.mining.soft.application.spi.IAuthenticationPort;
import com.mining.soft.application.spi.IPasswordEncoderPort;
import com.mining.soft.application.spi.IRolePersistencePort;
import com.mining.soft.application.spi.ISecurityContextPort;
import com.mining.soft.application.spi.IUserPersistencePort;
import com.mining.soft.application.usecase.AuthenticationUseCase;
import com.mining.soft.application.usecase.UserUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public IUserServicePort userServicePort(
            IUserPersistencePort userPersistencePort,
            IPasswordEncoderPort passwordEncoderPort,
            IRolePersistencePort rolePersistencePort,
            ISecurityContextPort securityContextPort) {
        return new UserUseCase(userPersistencePort, passwordEncoderPort, rolePersistencePort, securityContextPort);
    }

    @Bean
    public IAuthenticationServicePort authenticationServicePort(IAuthenticationPort authenticationPort) {
        return new AuthenticationUseCase(authenticationPort);
    }
}

