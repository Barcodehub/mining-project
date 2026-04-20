package com.mining.soft.infrastructure.out.jpa.adapter;

import com.mining.soft.application.spi.ICommentPersistencePort;
import com.mining.soft.domain.model.Comment;
import com.mining.soft.infrastructure.out.jpa.entity.CommentEntity;
import com.mining.soft.infrastructure.out.jpa.mapper.ICommentEntityMapper;
import com.mining.soft.infrastructure.out.jpa.repository.ICommentRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class CommentJpaAdapter implements ICommentPersistencePort {

    private final ICommentRepository commentRepository;
    private final ICommentEntityMapper commentEntityMapper;

    @Override
    public Comment saveComment(Comment comment) {
        CommentEntity saved = commentRepository.save(commentEntityMapper.toEntity(comment));
        return commentEntityMapper.toModel(saved);
    }

    @Override
    public List<Comment> findCommentsByPostId(Long postId) {
        return commentEntityMapper.toModelList(commentRepository.findByPostId(postId));
    }
}
