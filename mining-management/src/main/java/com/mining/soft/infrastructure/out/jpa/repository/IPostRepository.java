package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.model.ReportAnalytics;
import com.mining.soft.infrastructure.out.jpa.entity.PostEntity;
import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;import org.springframework.data.domain.Page;
import java.util.List;


@Repository
public interface IPostRepository extends JpaRepository<PostEntity, Long> {

    @Query("SELECT p FROM PostEntity p LEFT JOIN FETCH p.images")
    Page<PostEntity> findAllWithImage(Pageable pageable);

    @Query("SELECT new com.mining.soft.domain.model.ReportAnalytics(" +
           "r.clusterId, " +
           "COUNT(r), " +
           "SUM(CASE WHEN r.status = 'VALIDADO' THEN 1.0 " +
                "WHEN r.status = 'PENDIENTE' THEN 0.5 " +
                "WHEN r.status = 'RECHAZADO' THEN 0.1 " +
                "ELSE 0.0 END)) " +
           "FROM ReportEntity r " +
           "WHERE r.clusterId IS NOT NULL " +
           "GROUP BY r.clusterId")
    List<ReportAnalytics> getReportAnalytics();
}
