package com.mining.soft.application.usecase;


import com.mining.soft.application.api.IUserServicePort;
import com.mining.soft.domain.enums.RoleEnum;
import com.mining.soft.domain.exception.UnauthorizedRoleCreationException;
import com.mining.soft.domain.exception.UserUnderageException;
import com.mining.soft.domain.model.RoleModel;
import com.mining.soft.domain.model.UserModel;
import com.mining.soft.application.spi.IPasswordEncoderPort;
import com.mining.soft.application.spi.IRolePersistencePort;
import com.mining.soft.application.spi.ISecurityContextPort;
import com.mining.soft.application.spi.IUserPersistencePort;
import com.mining.soft.infrastructure.exception.NoDataFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.Period;

@RequiredArgsConstructor
public class UserUseCase implements IUserServicePort {

    private final IUserPersistencePort userPersistencePort;
    private final IPasswordEncoderPort passwordEncoderPort;
    private final IRolePersistencePort rolePersistencePort;
    private final ISecurityContextPort securityContextPort;

    private static final int MINIMUM_AGE = 18;


    @Override
    public UserModel createUser(UserModel userModel) {
        RoleModel role = rolePersistencePort.findById(userModel.getRole().getId())
                .orElseThrow(() -> new NoDataFoundException("Rol no encontrado. roleID: " + userModel.getRole().getId()));
        userModel.setRole(role);

            validateRoleCreationPermissions(role);


        if (role.getId().equals(RoleEnum.INGENIERO.getRoleId())) {
            validateAge(userModel.getBirthDate());
        }

        String encodedPassword = passwordEncoderPort.encode(userModel.getPassword());
        userModel.setPassword(encodedPassword);

        return userPersistencePort.saveUser(userModel);
    }

    @Override
    public Page<UserModel> getAllUsers(Pageable pageable) {
        return userPersistencePort.findAll(pageable);
    }

    @Override
    public UserModel getUserById(Long id) {
        return userPersistencePort.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }


    private void validateAge(LocalDate birthDate) {
        LocalDate today = LocalDate.now();
        int age = Period.between(birthDate, today).getYears();

        if (age < MINIMUM_AGE) {
            throw new UserUnderageException(
                String.format("El usuario debe ser mayor de edad. Edad actual: %d años", age)
            );
        }
    }

    private String getCurrentUserRoleSafe() {
        try {
            return securityContextPort.getCurrentUserRole();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void validateRoleCreationPermissions(RoleModel roleToCreate) {
        if (roleToCreate == null || roleToCreate.getId() == null) {
            throw new IllegalArgumentException("El rol del usuario a crear es requerido");
        }

        String currentUserRole = getCurrentUserRoleSafe();

        if (currentUserRole == null) {
            if (roleToCreate.getId().equals(RoleEnum.CLIENTE.getRoleId())) {
                return;
            } else {
                throw new UnauthorizedRoleCreationException(
                    "Usuarios no autenticados solo pueden crear usuarios con rol CLIENTE"
                );
            }
        }
        Long roleIdToCreate = roleToCreate.getId();

        // Si intenta crear un INGENIERO, debe ser ADMINISTRADOR
        if (roleIdToCreate.equals(RoleEnum.INGENIERO.getRoleId())
                && !RoleEnum.ADMINISTRADOR.getName().equalsIgnoreCase(currentUserRole)) {
            throw new UnauthorizedRoleCreationException(
                "Solo los usuarios con rol ADMINISTRADOR pueden crear usuarios INGENIERO"
            );
        }

        // Si intenta crear un MODERADOR, debe ser INGENIERO
        if (roleIdToCreate.equals(RoleEnum.MODERADOR.getRoleId())
                && !RoleEnum.INGENIERO.getName().equalsIgnoreCase(currentUserRole)) {
            throw new UnauthorizedRoleCreationException(
                "Solo los usuarios con rol INGENIERO pueden crear usuarios MODERADOR"
            );
        }

        // No se permite crear ADMINISTRADORES ni CLIENTES
        if (roleIdToCreate.equals(RoleEnum.ADMINISTRADOR.getRoleId()) || roleIdToCreate.equals(RoleEnum.CLIENTE.getRoleId())) {
            throw new UnauthorizedRoleCreationException(
                "No se permite crear usuarios con este rol "
            );
        }

    }
}
