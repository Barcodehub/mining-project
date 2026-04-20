package com.mining.soft.infrastructure.input;

import com.mining.soft.application.api.ICommentServicePort;
import com.mining.soft.infrastructure.input.dto.CommentRequestDto;
import com.mining.soft.infrastructure.input.dto.CommentResponseDto;
import com.mining.soft.infrastructure.input.mapper.ICommentDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

    private final ICommentServicePort commentServicePort;
    private final ICommentDtoMapper commentDtoMapper;

    @PostMapping
    public ResponseEntity<CommentResponseDto> createComment(@RequestBody CommentRequestDto request) {
        return new ResponseEntity<>(
                commentDtoMapper.toResponseDto(commentServicePort.createComment(commentDtoMapper.toModel(request))),
                HttpStatus.CREATED
        );
    }
}
