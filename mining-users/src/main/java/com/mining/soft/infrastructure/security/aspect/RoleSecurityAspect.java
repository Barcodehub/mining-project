package com.mining.soft.infrastructure.security.aspect;

import com.mining.soft.infrastructure.security.annotations.RequireRole;
import com.mining.soft.infrastructure.security.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class RoleSecurityAspect {

    private final SecurityContextUtil securityContextUtil;

    @Before("@annotation(requireRole)")
    public void checkRole(RequireRole requireRole) {
        String currentRole = securityContextUtil.getCurrentUserRole();

        boolean hasRequiredRole = Arrays.stream(requireRole.value())
                .anyMatch(role -> role.getName().equalsIgnoreCase(currentRole));

        if (!hasRequiredRole) {
            throw new AccessDeniedException("No tiene permisos para realizar esta acción. Rol requerido: "
                + Arrays.toString(requireRole.value()));
        }
    }
}
