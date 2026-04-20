package com.mining.soft.application.usecase;

import com.mining.soft.application.api.IPostServicePort;
import com.mining.soft.application.spi.IPostPersistencePort;
import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public class PostUseCase implements IPostServicePort {

    private final IPostPersistencePort postPersistencePort;

    public PostUseCase(IPostPersistencePort postPersistencePort) {
        this.postPersistencePort = postPersistencePort;
    }

    @Override
    public Post createPost(Post post) {
        post.validate();
        return postPersistencePort.savePost(post);
    }

    @Override
    public List<Report> getAllReports() {
        return postPersistencePort.findAllReports();
    }

    @Override
    public Report getReportById(Long id) {
        return postPersistencePort.findReportById(id);
    }

    @Override
    public Page<Post> getAllPosts(Pageable pageable) {
        return postPersistencePort.findAllPosts(pageable);
    }

    @Override
    public Report requestValidation(Long reportId) {
        Report report = postPersistencePort.findReportById(reportId);
        report.requestValidation();
        return (Report) postPersistencePort.savePost(report);
    }

    @Override
    public Report changeReportStatus(Long reportId, ReportStatus newStatus, Long validatorId) {
        Report report = postPersistencePort.findReportById(reportId);
        report.updateStatus(newStatus, validatorId);
        return (Report) postPersistencePort.savePost(report);
    }

    @Override
    public List<Report> filterReports(ReportStatus status, LocalDateTime startDate, LocalDateTime endDate, Double minLat, Double maxLat, Double minLng, Double maxLng) {
        return postPersistencePort.findFilteredReports(status, startDate, endDate, minLat, maxLat, minLng, maxLng);
    }
}
