package com.mining.soft.application.api;

import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface IPostServicePort {
    Post createPost(Post post);
    List<Report> getAllReports();
    Report getReportById(Long id);

    Page<Post> getAllPosts(Pageable pageable);
    Report requestValidation(Long reportId);
    Report changeReportStatus(Long reportId, ReportStatus newStatus, Long validatorId);
    List<Report> filterReports(ReportStatus status, LocalDateTime startDate, LocalDateTime endDate, Double minLat, Double maxLat, Double minLng, Double maxLng);
}
