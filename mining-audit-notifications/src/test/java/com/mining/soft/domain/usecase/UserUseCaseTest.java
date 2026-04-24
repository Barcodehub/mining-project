package com.mining.soft.domain.usecase;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserUseCase - Gestión de Usuarios")
class UserUseCaseTest {

    @Mock
    private IUserPersistencePort userPersistencePort;

    @Mock
    private IPasswordEncoderPort passwordEncoderPort;

    @Mock
    private IRolePersistencePort rolePersistencePort;

    @Mock
    private ISecurityContextPort securityContextPort;

    @InjectMocks
    private UserUseCase userUseCase;

    private RoleModel adminRole;
    private RoleModel ownerRole;
    private RoleModel employeeRole;
    private RoleModel clientRole;

    @BeforeEach
    void setUp() {
        adminRole = new RoleModel(RoleEnum.ADMINISTRADOR.getRoleId(), RoleEnum.ADMINISTRADOR.getName(), "Admin");
        ownerRole = new RoleModel(RoleEnum.INGENIERO.getRoleId(), RoleEnum.INGENIERO.getName(), "Owner");
        employeeRole = new RoleModel(RoleEnum.MODERADOR.getRoleId(), RoleEnum.MODERADOR.getName(), "Employee");
        clientRole = new RoleModel(RoleEnum.CLIENTE.getRoleId(), RoleEnum.CLIENTE.getName(), "Client");
    }

    @Nested
    @DisplayName("HU1: Crear Propietario")
    class CreateOwnerTests {

        @Test
        @DisplayName("Happy Path: Debe crear ingeniero mayor de edad correctamente")
        void shouldCreateOwnerWhenUserIsAdultAndAdminIsAuthenticated() {
            UserModel userModel = createUserModel(ownerRole, LocalDate.now().minusYears(25));

            when(rolePersistencePort.findById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
            when(securityContextPort.getCurrentUserRole()).thenReturn(RoleEnum.ADMINISTRADOR.getName());
            when(passwordEncoderPort.encode(anyString())).thenReturn("encodedPassword");
            when(userPersistencePort.saveUser(any(UserModel.class))).thenReturn(userModel);

            UserModel result = userUseCase.createUser(userModel);

            assertNotNull(result);
            verify(rolePersistencePort).findById(ownerRole.getId());
            verify(passwordEncoderPort).encode(anyString());
            verify(userPersistencePort).saveUser(any(UserModel.class));
        }

        @Test
        @DisplayName("Validación: Debe rechazar ingeniero menor de edad")
        void shouldThrowExceptionWhenOwnerIsUnderage() {
            UserModel userModel = createUserModel(ownerRole, LocalDate.now().minusYears(17));

            when(rolePersistencePort.findById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
            when(securityContextPort.getCurrentUserRole()).thenReturn(RoleEnum.ADMINISTRADOR.getName());

            assertThrows(UserUnderageException.class, () -> userUseCase.createUser(userModel));
            verify(userPersistencePort, never()).saveUser(any(UserModel.class));
        }

        @Test
        @DisplayName("Error: Debe rechazar creación de ingeniero sin rol administrador")
        void shouldThrowExceptionWhenNonAdminTriesToCreateOwner() {
            UserModel userModel = createUserModel(ownerRole, LocalDate.now().minusYears(25));

            when(rolePersistencePort.findById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
            when(securityContextPort.getCurrentUserRole()).thenReturn(RoleEnum.INGENIERO.getName());

            assertThrows(UnauthorizedRoleCreationException.class, () -> userUseCase.createUser(userModel));
            verify(userPersistencePort, never()).saveUser(any(UserModel.class));
        }
    }

    @Nested
    @DisplayName("HU6: Crear cuenta moderador")
    class CreateEmployeeTests {

        @Test
        @DisplayName("Happy Path: Debe crear moderador cuando ingeniero está autenticado")
        void shouldCreateEmployeeWhenOwnerIsAuthenticated() {
            UserModel userModel = createUserModel(employeeRole, LocalDate.now().minusYears(20));

            when(rolePersistencePort.findById(employeeRole.getId())).thenReturn(Optional.of(employeeRole));
            when(securityContextPort.getCurrentUserRole()).thenReturn(RoleEnum.INGENIERO.getName());
            when(passwordEncoderPort.encode(anyString())).thenReturn("encodedPassword");
            when(userPersistencePort.saveUser(any(UserModel.class))).thenReturn(userModel);

            UserModel result = userUseCase.createUser(userModel);

            assertNotNull(result);
            assertEquals(employeeRole, result.getRole());
            verify(passwordEncoderPort).encode(anyString());
            verify(userPersistencePort).saveUser(any(UserModel.class));
        }

        @Test
        @DisplayName("Validación: Debe rechazar creación de moderador por usuario no ingeniero")
        void shouldThrowExceptionWhenNonOwnerTriesToCreateEmployee() {
            UserModel userModel = createUserModel(employeeRole, LocalDate.now().minusYears(20));

            when(rolePersistencePort.findById(employeeRole.getId())).thenReturn(Optional.of(employeeRole));
            when(securityContextPort.getCurrentUserRole()).thenReturn(RoleEnum.ADMINISTRADOR.getName());

            assertThrows(UnauthorizedRoleCreationException.class, () -> userUseCase.createUser(userModel));
            verify(userPersistencePort, never()).saveUser(any(UserModel.class));
        }

        @Test
        @DisplayName("Error: Debe fallar cuando el rol no existe en el sistema")
        void shouldThrowExceptionWhenRoleNotFound() {
            UserModel userModel = createUserModel(employeeRole, LocalDate.now().minusYears(20));

            when(rolePersistencePort.findById(employeeRole.getId())).thenReturn(Optional.empty());

            assertThrows(NoDataFoundException.class, () -> userUseCase.createUser(userModel));
            verify(userPersistencePort, never()).saveUser(any(UserModel.class));
        }
    }

    @Nested
    @DisplayName("HU8: Crear cuenta Cliente")
    class CreateClientTests {

        @Test
        @DisplayName("Happy Path: Debe crear cliente sin autenticación")
        void shouldCreateClientWithoutAuthentication() {
            UserModel userModel = createUserModel(clientRole, LocalDate.now().minusYears(20));

            when(rolePersistencePort.findById(clientRole.getId())).thenReturn(Optional.of(clientRole));
            when(securityContextPort.getCurrentUserRole()).thenThrow(new RuntimeException("No authenticated"));
            when(passwordEncoderPort.encode(anyString())).thenReturn("encodedPassword");
            when(userPersistencePort.saveUser(any(UserModel.class))).thenReturn(userModel);

            UserModel result = userUseCase.createUser(userModel);

            assertNotNull(result);
            assertEquals(clientRole, result.getRole());
            verify(passwordEncoderPort).encode(anyString());
            verify(userPersistencePort).saveUser(any(UserModel.class));
        }

        @Test
        @DisplayName("Validación: Debe crear cliente con cualquier edad válida")
        void shouldCreateClientWithAnyAge() {
            UserModel userModel = createUserModel(clientRole, LocalDate.now().minusYears(16));

            when(rolePersistencePort.findById(clientRole.getId())).thenReturn(Optional.of(clientRole));
            when(securityContextPort.getCurrentUserRole()).thenThrow(new RuntimeException("No authenticated"));
            when(passwordEncoderPort.encode(anyString())).thenReturn("encodedPassword");
            when(userPersistencePort.saveUser(any(UserModel.class))).thenReturn(userModel);

            UserModel result = userUseCase.createUser(userModel);

            assertNotNull(result);
            verify(userPersistencePort).saveUser(any(UserModel.class));
        }

        @Test
        @DisplayName("Error: Usuario no autenticado no puede crear otros roles")
        void shouldThrowExceptionWhenUnauthenticatedUserTriesToCreateNonClientRole() {
            UserModel userModel = createUserModel(ownerRole, LocalDate.now().minusYears(25));

            when(rolePersistencePort.findById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
            when(securityContextPort.getCurrentUserRole()).thenThrow(new RuntimeException("No authenticated"));

            assertThrows(UnauthorizedRoleCreationException.class, () -> userUseCase.createUser(userModel));
            verify(userPersistencePort, never()).saveUser(any(UserModel.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases y Validaciones Adicionales")
    class EdgeCasesTests {

        @Test
        @DisplayName("Validación: Debe rechazar creación de usuario administrador")
        void shouldThrowExceptionWhenTryingToCreateAdmin() {
            UserModel userModel = createUserModel(adminRole, LocalDate.now().minusYears(30));

            when(rolePersistencePort.findById(adminRole.getId())).thenReturn(Optional.of(adminRole));
            when(securityContextPort.getCurrentUserRole()).thenReturn(RoleEnum.ADMINISTRADOR.getName());

            assertThrows(UnauthorizedRoleCreationException.class, () -> userUseCase.createUser(userModel));
            verify(userPersistencePort, never()).saveUser(any(UserModel.class));
        }

        @Test
        @DisplayName("Validación: Debe encriptar la contraseña antes de guardar")
        void shouldEncryptPasswordBeforeSaving() {
            String plainPassword = "plainPassword123";
            String encodedPassword = "encodedPassword456";
            UserModel userModel = createUserModel(clientRole, LocalDate.now().minusYears(20));
            userModel.setPassword(plainPassword);

            when(rolePersistencePort.findById(clientRole.getId())).thenReturn(Optional.of(clientRole));
            when(securityContextPort.getCurrentUserRole()).thenThrow(new RuntimeException("No authenticated"));
            when(passwordEncoderPort.encode(plainPassword)).thenReturn(encodedPassword);
            when(userPersistencePort.saveUser(any(UserModel.class))).thenReturn(userModel);

            userUseCase.createUser(userModel);

            verify(passwordEncoderPort).encode(plainPassword);
            assertEquals(encodedPassword, userModel.getPassword());
        }

        @Test
        @DisplayName("Edge Case: Propietario exactamente de 18 años debe ser aceptado")
        void shouldAcceptOwnerExactly18YearsOld() {
            UserModel userModel = createUserModel(ownerRole, LocalDate.now().minusYears(18));

            when(rolePersistencePort.findById(ownerRole.getId())).thenReturn(Optional.of(ownerRole));
            when(securityContextPort.getCurrentUserRole()).thenReturn(RoleEnum.ADMINISTRADOR.getName());
            when(passwordEncoderPort.encode(anyString())).thenReturn("encodedPassword");
            when(userPersistencePort.saveUser(any(UserModel.class))).thenReturn(userModel);

            UserModel result = userUseCase.createUser(userModel);

            assertNotNull(result);
            verify(userPersistencePort).saveUser(any(UserModel.class));
        }

        @Test
        @DisplayName("Validación: Debe fallar si el rol del usuario es null")
        void shouldThrowExceptionWhenRoleIsNull() {
            UserModel userModel = createUserModel(null, LocalDate.now().minusYears(20));

            assertThrows(NullPointerException.class, () -> userUseCase.createUser(userModel));
        }
    }

    private UserModel createUserModel(RoleModel role, LocalDate birthDate) {
        UserModel user = new UserModel();
        user.setName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setIdentificationDocument("123456789");
        user.setPhoneNumber("+573001234567");
        user.setBirthDate(birthDate);
        user.setRole(role);
        return user;
    }
}
