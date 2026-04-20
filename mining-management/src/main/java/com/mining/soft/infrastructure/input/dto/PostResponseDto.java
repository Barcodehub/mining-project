package com.mining.soft.infrastructure.input.dto;
import com.mining.soft.domain.enums.PostType;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class PostResponseDto {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private PostType type;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private List<PostImageDto> images;
}
