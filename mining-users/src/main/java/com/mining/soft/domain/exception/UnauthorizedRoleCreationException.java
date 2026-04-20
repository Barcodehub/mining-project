package com.mining.soft.domain.exception;

public class UnauthorizedRoleCreationException extends RuntimeException {
    public UnauthorizedRoleCreationException(String message) {
        super(message);
    }
}

