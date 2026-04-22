package com.mining.soft.infrastructure.out.jpa.mapper;

import com.mining.soft.domain.model.Comment;
import com.mining.soft.infrastructure.out.jpa.entity.CommentEntity;
import com.mining.soft.infrastructure.out.jpa.entity.PostEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ICommentEntityMapper {
    @Mapping(target = "post", source = "postId")
    CommentEntity toEntity(Comment comment);

    @Mapping(target = "postId", source = "post.id")
    Comment toModel(CommentEntity commentEntity);

    List<Comment> toModelList(List<CommentEntity> commentEntities);

    default PostEntity map(Long postId) {
        if (postId == null) {
            return null;
        }
        PostEntity post = new PostEntity();
        post.setId(postId);
        return post;
    }

    default Long map(PostEntity post) {
        return post != null ? post.getId() : null;
    }
}
