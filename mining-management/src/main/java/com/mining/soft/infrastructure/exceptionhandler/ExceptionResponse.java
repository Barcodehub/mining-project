package com.mining.soft.infrastructure.exceptionhandler;

public enum ExceptionResponse {
    NO_DATA_FOUND("No se encontraron datos para la petición solicitada"),

    INTERNAL_SERVER_ERROR("Ha ocurrido un error inesperado. Por favor, contacte al administrador");

    private final String message;

    ExceptionResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
}
