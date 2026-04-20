package com.mining.soft.infrastructure.out.security;

import com.mining.soft.application.spi.ISecurityContextPort;
import com.mining.soft.infrastructure.security.util.SecurityContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextAdapter implements ISecurityContextPort {

    private final SecurityContextUtil securityContextUtil;

    @Override
    public Long getCurrentUserId() {
        return securityContextUtil.getCurrentUserId();
    }

    @Override
    public String getCurrentUserRole() {
        return securityContextUtil.getCurrentUserRole();
    }

    @Override
    public boolean hasRole(String roleName) {
        return securityContextUtil.hasRole(roleName);
    }
}

