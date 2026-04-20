package com.mining.soft.infrastructure.exceptionhandler;

public enum ExceptionResponse {
    NO_DATA_FOUND("No se encontraron datos para la petición solicitada"),
    USER_UNDERAGE("El usuario debe ser mayor de 18 años para registrarse"),
    INVALID_USER_DATA("Los datos del usuario son inválidos"),
    USER_ALREADY_EXISTS("El usuario ya existe en el sistema"),
    EMAIL_ALREADY_EXISTS("Ya existe un usuario registrado con ese email"),
    IDENTIFICATION_ALREADY_EXISTS("Ya existe un usuario registrado con ese documento de identidad"),
    DUPLICATE_USER_DATA("El email o documento de identidad ya están registrados"),
    VALIDATION_FAILED("Errores de validación en los campos proporcionados"),
    INTERNAL_SERVER_ERROR("Ha ocurrido un error inesperado. Por favor, contacte al administrador");

    private final String message;

    ExceptionResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
