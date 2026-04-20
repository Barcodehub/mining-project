package com.mining.soft.infrastructure.security.util;

import com.mining.soft.domain.model.RoleModel;
import com.mining.soft.infrastructure.security.userdetails.CustomUserDetails;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextUtil {

    public CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        throw new AuthenticationCredentialsNotFoundException("Usuario no autenticado");
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public String getCurrentUserRole() {
        RoleModel role = getCurrentUser().getRole();
        return role != null ? role.getName() : null;
    }

    public String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    public boolean hasRole(String roleName) {
        try {
            String currentRole = getCurrentUserRole();
            return currentRole != null && currentRole.equalsIgnoreCase(roleName);
        } catch (Exception e) {
            return false;
        }
    }
}
