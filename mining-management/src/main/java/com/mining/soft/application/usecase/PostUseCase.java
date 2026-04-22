package com.mining.soft.application.usecase;

import com.mining.soft.application.api.IPostServicePort;
import com.mining.soft.application.spi.IPostPersistencePort;
import com.mining.soft.application.spi.IPublishEventPort;
import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public class PostUseCase implements IPostServicePort {

    private final IPostPersistencePort postPersistencePort;
    private final IPublishEventPort publishEventPort;

    public PostUseCase(IPostPersistencePort postPersistencePort, IPublishEventPort publishEventPort) {
        this.postPersistencePort = postPersistencePort;
        this.publishEventPort = publishEventPort;
    }

    @Override
    public Post createPost(Post post) {
        post.validate();
        Post savedPost = postPersistencePort.savePost(post);

        if (savedPost instanceof Report report) {
            publishEventPort.publish("REPORT_CREATED", report.getId(), report.getUserId(), null, report.getCreatedAt(), java.util.Map.of("title", report.getTitle()));
        }
        return savedPost;
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
        Report savedReport = (Report) postPersistencePort.savePost(report);

        String eventType = newStatus == ReportStatus.VALIDADO ? "REPORT_VALIDATED" :
                           newStatus == ReportStatus.RECHAZADO ? "REPORT_REJECTED" : "REPORT_STATUS_CHANGED";

        publishEventPort.publish(eventType, savedReport.getId(), validatorId, savedReport.getUserId(), LocalDateTime.now(), java.util.Map.of("newStatus", newStatus.name()));

        return savedReport;
    }

    @Override
    public List<Report> filterReports(ReportStatus status, LocalDateTime startDate, LocalDateTime endDate, Double minLat, Double maxLat, Double minLng, Double maxLng) {
        return postPersistencePort.findFilteredReports(status, startDate, endDate, minLat, maxLat, minLng, maxLng);
    }
}
