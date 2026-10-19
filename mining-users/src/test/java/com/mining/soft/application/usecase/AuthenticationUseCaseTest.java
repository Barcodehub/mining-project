package com.mining.soft.application.usecase;

import com.mining.soft.domain.model.RoleModel;
import com.mining.soft.domain.model.auth.AuthResponse;
import com.mining.soft.domain.model.auth.LoginRequest;
import com.mining.soft.application.spi.IAuthenticationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationUseCase - Autenticación de Usuarios")
class AuthenticationUseCaseTest {

    @Mock
    private IAuthenticationPort authenticationPort;

    @InjectMocks
    private AuthenticationUseCase authenticationUseCase;

    private LoginRequest validLoginRequest;
    private AuthResponse validAuthResponse;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest("user@example.com", "password123");

        RoleModel clientRole = new RoleModel(4L, "CLIENTE", "Client");
        validAuthResponse = new AuthResponse(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                1L,
                "user@example.com",
                clientRole
        );
    }

    @Nested
    @DisplayName("HU5: Autenticación al sistema")
    class LoginTests {

        @Test
        @DisplayName("Happy Path: Debe autenticar usuario con credenciales válidas")
        void shouldAuthenticateUserWithValidCredentials() {
            when(authenticationPort.authenticate(any(LoginRequest.class))).thenReturn(validAuthResponse);

            AuthResponse result = authenticationUseCase.login(validLoginRequest);

            assertNotNull(result);
            assertNotNull(result.getToken());
            assertEquals("user@example.com", result.getEmail());
            assertEquals(1L, result.getUserId());
            assertEquals("CLIENTE", result.getRole().getName());
            assertEquals("Bearer", result.getType());

            verify(authenticationPort, times(1)).authenticate(validLoginRequest);
        }

        @Test
        @DisplayName("Validación: Debe rechazar credenciales inválidas")
        void shouldThrowExceptionWhenCredentialsAreInvalid() {
            LoginRequest invalidRequest = new LoginRequest("user@example.com", "wrongPassword");
            when(authenticationPort.authenticate(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Credenciales inválidas"));

            assertThrows(BadCredentialsException.class, () -> authenticationUseCase.login(invalidRequest));
            verify(authenticationPort, times(1)).authenticate(invalidRequest);
        }

        @Test
        @DisplayName("Error: Debe rechazar usuario no existente")
        void shouldThrowExceptionWhenUserDoesNotExist() {
            LoginRequest nonExistentUserRequest = new LoginRequest("nonexistent@example.com", "password123");
            when(authenticationPort.authenticate(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Credenciales inválidas"));

            BadCredentialsException exception = assertThrows(
                    BadCredentialsException.class,
                    () -> authenticationUseCase.login(nonExistentUserRequest)
            );

            assertEquals("Credenciales inválidas", exception.getMessage());
            verify(authenticationPort, times(1)).authenticate(nonExistentUserRequest);
        }
    }

    @Nested
    @DisplayName("Validaciones de Login Request")
    class LoginRequestValidationTests {

        @Test
        @DisplayName("Validación: Debe autenticar diferentes roles correctamente")
        void shouldAuthenticateDifferentRoles() {
            RoleModel ownerRole = new RoleModel(2L, "INGENIERO", "Owner");
            AuthResponse ownerAuthResponse = new AuthResponse(
                    "ownerToken",
                    2L,
                    "owner@example.com",
                    ownerRole
            );
            LoginRequest ownerLogin = new LoginRequest("owner@example.com", "ownerPass");

            when(authenticationPort.authenticate(ownerLogin)).thenReturn(ownerAuthResponse);

            AuthResponse result = authenticationUseCase.login(ownerLogin);

            assertNotNull(result);
            assertEquals("INGENIERO", result.getRole().getName());
            assertEquals(2L, result.getUserId());
        }

        @Test
        @DisplayName("Validación: Debe autenticar moderador correctamente")
        void shouldAuthenticateEmployee() {
            RoleModel employeeRole = new RoleModel(3L, "MODERADOR", "Employee");
            AuthResponse employeeAuthResponse = new AuthResponse(
                    "employeeToken",
                    3L,
                    "employee@example.com",
                    employeeRole
            );
            LoginRequest employeeLogin = new LoginRequest("employee@example.com", "employeePass");

            when(authenticationPort.authenticate(employeeLogin)).thenReturn(employeeAuthResponse);

            AuthResponse result = authenticationUseCase.login(employeeLogin);

            assertNotNull(result);
            assertEquals("MODERADOR", result.getRole().getName());
        }

        @Test
        @DisplayName("Error: Debe fallar con email vacío")
        void shouldFailWithEmptyEmail() {
            LoginRequest emptyEmailRequest = new LoginRequest("", "password123");
            when(authenticationPort.authenticate(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Credenciales inválidas"));

            assertThrows(BadCredentialsException.class, () -> authenticationUseCase.login(emptyEmailRequest));
        }

        @Test
        @DisplayName("Error: Debe fallar con password vacío")
        void shouldFailWithEmptyPassword() {
            LoginRequest emptyPasswordRequest = new LoginRequest("user@example.com", "");
            when(authenticationPort.authenticate(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Credenciales inválidas"));

            assertThrows(BadCredentialsException.class, () -> authenticationUseCase.login(emptyPasswordRequest));
        }

        @Test
        @DisplayName("Validación: Token JWT debe tener formato correcto")
        void shouldReturnValidJwtTokenFormat() {
            when(authenticationPort.authenticate(any(LoginRequest.class))).thenReturn(validAuthResponse);

            AuthResponse result = authenticationUseCase.login(validLoginRequest);

            assertNotNull(result.getToken());
            assertFalse(result.getToken().isEmpty());
            assertTrue(result.getToken().contains("."), "JWT token should contain dots");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Edge Case: Email con mayúsculas debe funcionar")
        void shouldAuthenticateWithUppercaseEmail() {
            LoginRequest upperCaseEmailRequest = new LoginRequest("USER@EXAMPLE.COM", "password123");
            when(authenticationPort.authenticate(any(LoginRequest.class))).thenReturn(validAuthResponse);

            AuthResponse result = authenticationUseCase.login(upperCaseEmailRequest);

            assertNotNull(result);
            verify(authenticationPort, times(1)).authenticate(upperCaseEmailRequest);
        }

        @Test
        @DisplayName("Edge Case: Password con caracteres especiales debe funcionar")
        void shouldAuthenticateWithSpecialCharactersInPassword() {
            LoginRequest specialCharsPassword = new LoginRequest("user@example.com", "P@ssw0rd!#$%");
            when(authenticationPort.authenticate(any(LoginRequest.class))).thenReturn(validAuthResponse);

            AuthResponse result = authenticationUseCase.login(specialCharsPassword);

            assertNotNull(result);
            verify(authenticationPort, times(1)).authenticate(specialCharsPassword);
        }

        @Test
        @DisplayName("Edge Case: Debe manejar múltiples intentos fallidos")
        void shouldHandleMultipleFailedAttempts() {
            LoginRequest invalidRequest = new LoginRequest("user@example.com", "wrongPassword");
            when(authenticationPort.authenticate(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Credenciales inválidas"));

            assertThrows(BadCredentialsException.class, () -> authenticationUseCase.login(invalidRequest));

            assertThrows(BadCredentialsException.class, () -> authenticationUseCase.login(invalidRequest));

            assertThrows(BadCredentialsException.class, () -> authenticationUseCase.login(invalidRequest));

            verify(authenticationPort, times(3)).authenticate(invalidRequest);
        }
    }
}
