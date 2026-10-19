package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.model.ClusterAnalytics;
import com.mining.soft.infrastructure.out.jpa.entity.EnvironmentalDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEnvironmentalDataRepository
        extends JpaRepository<EnvironmentalDataEntity, Long>, EnvironmentalDataRepositoryCustom {

    /**
     * SAI por clúster: mediciones clasificadas (excluye INDETERMINADO), en ALERTA+CRITICO y en CRITICO.
     */
    @Query("SELECT new com.mining.soft.domain.model.ClusterAnalytics(" +
           "e.clusterId, " +
           "COUNT(e), " +
           "SUM(CASE WHEN e.alertLevel IN ('ALERTA', 'CRITICO') THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN e.alertLevel = 'CRITICO' THEN 1L ELSE 0L END)) " +
           "FROM EnvironmentalDataEntity e " +
           "WHERE e.clusterId IS NOT NULL AND e.alertLevel <> 'INDETERMINADO' " +
           "GROUP BY e.clusterId")
    List<ClusterAnalytics> getClusterAnalytics();
}
