package com.mining.soft.application.spi;

import com.mining.soft.domain.model.Comment;
import java.util.List;

public interface ICommentPersistencePort {
    Comment saveComment(Comment comment);
    List<Comment> findCommentsByPostId(Long postId);
}
