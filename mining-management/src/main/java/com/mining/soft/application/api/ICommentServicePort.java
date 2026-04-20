package com.mining.soft.application.api;

import com.mining.soft.domain.model.Comment;
import java.util.List;

public interface ICommentServicePort {
    Comment createComment(Comment comment);
    List<Comment> getCommentsByPostId(Long postId);
}
