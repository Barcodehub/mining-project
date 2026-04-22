package com.mining.soft.infrastructure.out.jpa.adapter.s3;


public enum BusinessErrorCodes {
    S3_NOT_FOUND("S3_NOT_FOUND", "Archivo no encontrado en S3");
    private final String code;
    private final String message;
    BusinessErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }
    public String code() {
        return code;
    }
    public String message() {
        return message;
    }
}
