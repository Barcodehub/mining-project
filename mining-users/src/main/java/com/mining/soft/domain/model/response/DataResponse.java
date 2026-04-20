package com.mining.soft.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DataResponse<T> {
    private T data;
    private String message;

    public DataResponse(T data) {
        this.data = data;
    }

    public static <T> DataResponse<T> of(T data) {
        return new DataResponse<>(data);
    }

    public static <T> DataResponse<T> of(T data, String message) {
        return new DataResponse<>(data, message);
    }
}
