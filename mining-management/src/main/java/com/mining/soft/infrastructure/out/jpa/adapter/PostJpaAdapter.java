package com.mining.soft.infrastructure.out.jpa.adapter;

import com.mining.soft.application.spi.IPostPersistencePort;
import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import com.mining.soft.infrastructure.out.jpa.entity.PostImageEntity;
import com.mining.soft.infrastructure.out.jpa.entity.PostEntity;
import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;
import com.mining.soft.infrastructure.out.jpa.mapper.IPostEntityMapper;
import com.mining.soft.infrastructure.out.jpa.repository.IPostRepository;
import com.mining.soft.infrastructure.out.jpa.repository.IReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class PostJpaAdapter implements IPostPersistencePort {

    private final IPostRepository postRepository;
    private final IReportRepository reportRepository;
    private final IPostEntityMapper postEntityMapper;

    @Override
    @Transactional
    @CacheEvict(value = "reports", allEntries = true)
    public Post savePost(Post post) {
        if (post instanceof Report) {
            ReportEntity reportEntity = postEntityMapper.toReportEntity((Report) post);
            attachImages(reportEntity);
            ReportEntity saved = reportRepository.save(reportEntity);
            return postEntityMapper.toReportModel(saved);
        } else {
            PostEntity postEntity = postEntityMapper.toEntity(post);
            attachImages(postEntity);
            PostEntity saved = postRepository.save(postEntity);
            return postEntityMapper.toModel(saved);
        }
    }

    private void attachImages(PostEntity postEntity) {
        if (postEntity.getImages() == null) {
            return;
        }
        for (PostImageEntity image : postEntity.getImages()) {
            image.setPost(postEntity);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> findAllReports() {
        return postEntityMapper.toReportModelList(reportRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public Report findReportById(Long id) {
        return reportRepository.findByIdWithImages(id)
                .map(postEntityMapper::toReportModel)
                .orElseThrow(() -> new IllegalArgumentException("Report no encontrado"));
    }

    @Override
    public boolean existsById(Long id) {
        return postRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Post> findAllPosts(Pageable pageable) {
        return postRepository.findAllWithImage(pageable).map(postEntityMapper::toModel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> findFilteredReports(ReportStatus status, LocalDateTime startDate, LocalDateTime endDate, Double minLat, Double maxLat, Double minLng, Double maxLng) {
        List<ReportEntity> entities = reportRepository.findFiltered(status, startDate, endDate, minLat, maxLat, minLng, maxLng);
        return postEntityMapper.toReportModelList(entities);
    }
}
