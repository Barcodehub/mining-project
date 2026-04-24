package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.model.ClusterAnalytics;
import com.mining.soft.infrastructure.out.jpa.entity.EnvironmentalDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEnvironmentalDataRepository extends JpaRepository<EnvironmentalDataEntity, Long>, EnvironmentalDataRepositoryCustom {

    @Query("SELECT new com.mining.soft.domain.model.ClusterAnalytics(" +
           "e.clusterId, " +
           "COUNT(e), " +
           "SUM(CASE WHEN e.mercury > 0.3 AND e.ph < 7.0 THEN 1 ELSE 0 END)) " +
           "FROM EnvironmentalDataEntity e " +
           "WHERE e.clusterId IS NOT NULL " +
           "GROUP BY e.clusterId")
    List<ClusterAnalytics> getClusterAnalytics();
}
