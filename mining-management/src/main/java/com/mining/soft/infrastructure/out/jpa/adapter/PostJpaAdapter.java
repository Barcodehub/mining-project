package com.mining.soft.infrastructure.out.jpa.adapter;

import com.mining.soft.application.spi.IPostPersistencePort;
import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import com.mining.soft.infrastructure.out.jpa.entity.PostEntity;
import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;
import com.mining.soft.infrastructure.out.jpa.mapper.IPostEntityMapper;
import com.mining.soft.infrastructure.out.jpa.repository.IPostRepository;
import com.mining.soft.infrastructure.out.jpa.repository.IReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class PostJpaAdapter implements IPostPersistencePort {

    private final IPostRepository postRepository;
    private final IReportRepository reportRepository;
    private final IPostEntityMapper postEntityMapper;

    @Override
    public Post savePost(Post post) {
        if (post instanceof Report) {
            ReportEntity saved = reportRepository.save(postEntityMapper.toReportEntity((Report) post));
            return postEntityMapper.toReportModel(saved);
        } else {
            PostEntity saved = postRepository.save(postEntityMapper.toEntity(post));
            return postEntityMapper.toModel(saved);
        }
    }

    @Override
    public List<Report> findAllReports() {
        return postEntityMapper.toReportModelList(reportRepository.findAll());
    }

    @Override
    public Report findReportById(Long id) {
        return reportRepository.findById(id)
                .map(postEntityMapper::toReportModel)
                .orElseThrow(() -> new IllegalArgumentException("Report no encontrado"));
    }

    @Override
    public boolean existsById(Long id) {
        return postRepository.existsById(id);
    }

    @Override
    public Page<Post> findAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable).map(postEntityMapper::toModel);
    }

    @Override
    public List<Report> findFilteredReports(ReportStatus status, LocalDateTime startDate, LocalDateTime endDate, Double minLat, Double maxLat, Double minLng, Double maxLng) {
        List<ReportEntity> entities = reportRepository.findFiltered(status, startDate, endDate, minLat, maxLat, minLng, maxLng);
        return postEntityMapper.toReportModelList(entities);
    }
}
