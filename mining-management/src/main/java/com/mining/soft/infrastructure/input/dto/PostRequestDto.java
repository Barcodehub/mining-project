package com.mining.soft.infrastructure.input.dto;
import com.mining.soft.domain.enums.PostType;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class PostRequestDto {
    private String title;
    private String content;
    private Long userId;
    private PostType type;
    private Double latitude;
    private Double longitude;
    private String clusterId;
    private List<PostImageDto> images;
}
