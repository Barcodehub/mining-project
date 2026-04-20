package com.mining.soft.application.spi;

public interface ISecurityContextPort {

    Long getCurrentUserId();
    String getCurrentUserRole();
    boolean hasRole(String roleName);
}

