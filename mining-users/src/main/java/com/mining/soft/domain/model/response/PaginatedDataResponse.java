package com.mining.soft.domain.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaginatedDataResponse<T> {
    private List<T> data;
    private PaginationMeta meta;
    private String message;

    public PaginatedDataResponse(List<T> data, PaginationMeta meta) {
        this.data = data;
        this.meta = meta;
        this.message = "Listado obtenido exitosamente";
    }

    public static <T> PaginatedDataResponse<T> of(List<T> data, PaginationMeta meta) {
        return new PaginatedDataResponse<>(data, meta);
    }

    public static <T> PaginatedDataResponse<T> of(List<T> data, PaginationMeta meta, String message) {
        return new PaginatedDataResponse<>(data, meta, message);
    }
}

