package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.infrastructure.out.jpa.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IReportRepository extends JpaRepository<ReportEntity, Long>, ReportRepositoryCustom {

    @Query("SELECT r FROM ReportEntity r LEFT JOIN FETCH r.images WHERE r.id = :id")
    Optional<ReportEntity> findByIdWithImages(Long id);
}
