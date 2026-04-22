package com.mining.soft.application.usecase;

import com.mining.soft.application.api.ICommentServicePort;
import com.mining.soft.application.spi.ICommentPersistencePort;
import com.mining.soft.application.spi.IPublishEventPort;
import com.mining.soft.application.spi.IPostPersistencePort;
import com.mining.soft.domain.model.Comment;
import java.util.List;

public class CommentUseCase implements ICommentServicePort {

    private final ICommentPersistencePort commentPersistencePort;
    private final IPostPersistencePort postPersistencePort;
    private final IPublishEventPort publishEventPort;

    public CommentUseCase(ICommentPersistencePort commentPersistencePort, IPostPersistencePort postPersistencePort, IPublishEventPort publishEventPort) {
        this.commentPersistencePort = commentPersistencePort;
        this.postPersistencePort = postPersistencePort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public Comment createComment(Comment comment) {
        if (!postPersistencePort.existsById(comment.getPostId())) {
            throw new IllegalArgumentException("El post no existe");
        }
        Comment savedComment = commentPersistencePort.saveComment(comment);

        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("postId", savedComment.getPostId());

        publishEventPort.publish(
            "COMMENT_CREATED",
            savedComment.getId(),
            savedComment.getUserId(),
            null, // Could get post owner user ID if needed
            savedComment.getCreatedAt() != null ? savedComment.getCreatedAt() : java.time.LocalDateTime.now(),
            metadata
        );

        return savedComment;
    }

    @Override
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentPersistencePort.findCommentsByPostId(postId);
    }
}
