package com.mining.soft.infrastructure.input.dto;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CommentRequestDto {
    private String content;
    private Long userId;
    private Long postId;
}
