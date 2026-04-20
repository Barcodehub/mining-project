package com.mining.soft.domain.model;

import com.mining.soft.domain.enums.PostType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class Post {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private PostType type;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private List<PostImage> images;

    public void validate() {
        if (this.type == PostType.REPORT) {
            if (this.latitude == null || this.longitude == null) {
                throw new IllegalArgumentException("Latitud y longitud son obligatorios para reportes");
            }
        }
    }
}
