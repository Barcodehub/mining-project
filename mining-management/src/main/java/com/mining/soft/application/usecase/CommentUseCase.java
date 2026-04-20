package com.mining.soft.application.usecase;

import com.mining.soft.application.api.ICommentServicePort;
import com.mining.soft.application.spi.ICommentPersistencePort;
import com.mining.soft.application.spi.IPostPersistencePort;
import com.mining.soft.domain.model.Comment;
import java.util.List;

public class CommentUseCase implements ICommentServicePort {

    private final ICommentPersistencePort commentPersistencePort;
    private final IPostPersistencePort postPersistencePort;

    public CommentUseCase(ICommentPersistencePort commentPersistencePort, IPostPersistencePort postPersistencePort) {
        this.commentPersistencePort = commentPersistencePort;
        this.postPersistencePort = postPersistencePort;
    }

    @Override
    public Comment createComment(Comment comment) {
        if (!postPersistencePort.existsById(comment.getPostId())) {
            throw new IllegalArgumentException("El post no existe");
        }
        return commentPersistencePort.saveComment(comment);
    }

    @Override
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentPersistencePort.findCommentsByPostId(postId);
    }
}
