package com.mining.soft.application.spi;

import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.domain.model.Post;
import com.mining.soft.domain.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface IPostPersistencePort {
    Post savePost(Post post);
    List<Report> findAllReports();
    Report findReportById(Long id);
    boolean existsById(Long id);

    Page<Post> findAllPosts(Pageable pageable);
    List<Report> findFilteredReports(ReportStatus status, LocalDateTime startDate, LocalDateTime endDate, Double minLat, Double maxLat, Double minLng, Double maxLng);
}
