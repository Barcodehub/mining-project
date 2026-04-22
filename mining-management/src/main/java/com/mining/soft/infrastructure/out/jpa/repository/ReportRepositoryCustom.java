package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.enums.ReportStatus;
import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepositoryCustom {

    List<ReportEntity> findFiltered(
            ReportStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng);
}

