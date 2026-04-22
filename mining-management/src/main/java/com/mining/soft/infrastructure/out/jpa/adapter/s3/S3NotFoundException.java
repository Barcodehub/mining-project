package com.mining.soft.infrastructure.out.jpa.adapter.s3;

public class S3NotFoundException extends RuntimeException {
    private final String code;
    public S3NotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }
    public String getCode() {
        return code;
    }
}
