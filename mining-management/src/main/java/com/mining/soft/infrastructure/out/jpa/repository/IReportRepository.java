package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IReportRepository extends JpaRepository<ReportEntity, Long> {

    @Query("SELECT r FROM ReportEntity r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:startDate IS NULL OR r.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR r.createdAt <= :endDate) AND " +
           "(:minLat IS NULL OR r.latitude >= :minLat) AND " +
           "(:maxLat IS NULL OR r.latitude <= :maxLat) AND " +
           "(:minLng IS NULL OR r.longitude >= :minLng) AND " +
           "(:maxLng IS NULL OR r.longitude <= :maxLng)")
    List<ReportEntity> findFiltered(
            @Param("status") ReportStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);
}
