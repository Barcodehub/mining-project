package com.mining.soft.infrastructure.out.jpa.mapper;

import com.mining.soft.domain.model.Comment;
import com.mining.soft.infrastructure.out.jpa.entity.CommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ICommentEntityMapper {
    @Mapping(target = "post.id", source = "postId")
    CommentEntity toEntity(Comment comment);

    @Mapping(target = "postId", source = "post.id")
    Comment toModel(CommentEntity commentEntity);

    List<Comment> toModelList(List<CommentEntity> commentEntities);
}
