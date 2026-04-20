package com.mining.soft.infrastructure.configuration;
import com.mining.soft.application.api.ICommentServicePort;
import com.mining.soft.application.api.IPostServicePort;
import com.mining.soft.application.spi.ICommentPersistencePort;
import com.mining.soft.application.spi.IPostPersistencePort;
import com.mining.soft.application.spi.IPublishEventPort;
import com.mining.soft.application.usecase.CommentUseCase;
import com.mining.soft.application.usecase.PostUseCase;
import com.mining.soft.infrastructure.out.jpa.adapter.CommentJpaAdapter;
import com.mining.soft.infrastructure.out.jpa.adapter.PostJpaAdapter;
import com.mining.soft.infrastructure.out.jpa.mapper.ICommentEntityMapper;
import com.mining.soft.infrastructure.out.jpa.mapper.IPostEntityMapper;
import com.mining.soft.infrastructure.out.jpa.repository.ICommentRepository;
import com.mining.soft.infrastructure.out.jpa.repository.IPostRepository;
import com.mining.soft.infrastructure.out.jpa.repository.IReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
@RequiredArgsConstructor
public class BeanConfiguration {
    private final IPostRepository postRepository;
    private final IReportRepository reportRepository;
    private final ICommentRepository commentRepository;
    private final IPostEntityMapper postEntityMapper;
    private final ICommentEntityMapper commentEntityMapper;
    private final IPublishEventPort publishEventPort;
    @Bean
    public IPostPersistencePort postPersistence() {
        return new PostJpaAdapter(postRepository, reportRepository, postEntityMapper);
    }
    @Bean
    public IPostServicePort postServicePort() {
        return new PostUseCase(postPersistence(), publishEventPort);
    }
    @Bean
    public ICommentPersistencePort commentPersistence() {
        return new CommentJpaAdapter(commentRepository, commentEntityMapper);
    }
    @Bean
    public ICommentServicePort commentServicePort() {
        return new CommentUseCase(commentPersistence(), postPersistence(), publishEventPort);
    }
}
