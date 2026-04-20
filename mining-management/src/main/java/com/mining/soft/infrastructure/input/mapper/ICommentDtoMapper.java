package com.mining.soft.infrastructure.input.mapper;

import com.mining.soft.domain.model.Comment;
import com.mining.soft.infrastructure.input.dto.CommentRequestDto;
import com.mining.soft.infrastructure.input.dto.CommentResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ICommentDtoMapper {
    Comment toModel(CommentRequestDto dto);
    CommentResponseDto toResponseDto(Comment comment);
    List<CommentResponseDto> toResponseDtoList(List<Comment> comments);
}
