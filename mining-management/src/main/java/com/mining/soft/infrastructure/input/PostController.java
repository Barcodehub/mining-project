package com.mining.soft.infrastructure.input;

import com.mining.soft.application.api.ICommentServicePort;
import com.mining.soft.application.api.IPostServicePort;
import com.mining.soft.domain.enums.PostType;
import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import com.mining.soft.infrastructure.input.dto.CommentResponseDto;
import com.mining.soft.infrastructure.input.dto.PostRequestDto;
import com.mining.soft.infrastructure.input.dto.PostResponseDto;
import com.mining.soft.infrastructure.input.mapper.ICommentDtoMapper;
import com.mining.soft.infrastructure.input.mapper.IPostDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final IPostServicePort postServicePort;
    private final ICommentServicePort commentServicePort;
    private final IPostDtoMapper postDtoMapper;
    private final ICommentDtoMapper commentDtoMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(value = "reports", allEntries = true)
    public ResponseEntity<PostResponseDto> createPost(@RequestBody PostRequestDto request) {
        Post post;
        if (request.getType() == PostType.REPORT) {
            Report report = postDtoMapper.toReportModel(request);
            post = postServicePort.createPost(report);
            return new ResponseEntity<>(postDtoMapper.toReportResponseDto((Report) post), HttpStatus.CREATED);
        } else {
            post = postDtoMapper.toModel(request);
            Post created = postServicePort.createPost(post);
            return new ResponseEntity<>(postDtoMapper.toResponseDto(created), HttpStatus.CREATED);
        }
    }

    @GetMapping("/{postId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(commentDtoMapper.toResponseDtoList(commentServicePort.getCommentsByPostId(postId)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<org.springframework.data.domain.Page<PostResponseDto>> getAllPosts(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(postServicePort.getAllPosts(pageable).map(postDtoMapper::toResponseDto));
    }
}
