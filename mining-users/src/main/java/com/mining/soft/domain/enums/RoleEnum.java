package com.mining.soft.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {
    ADMINISTRADOR(1L, "ADMINISTRADOR"),
    PROPIETARIO(2L, "PROPIETARIO"),
    EMPLEADO(3L, "EMPLEADO"),
    CLIENTE(4L, "CLIENTE");

    private final Long roleId;
    private final String name;

    public static RoleEnum fromString(String roleName) {
        for (RoleEnum role : RoleEnum.values()) {
            if (role.name.equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol no válido: " + roleName);
    }
}
