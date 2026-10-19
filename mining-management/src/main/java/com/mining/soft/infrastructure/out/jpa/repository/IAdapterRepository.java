package com.mining.soft.infrastructure.out.jpa.repository;
import com.mining.soft.infrastructure.out.jpa.entity.AdapterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface IAdapterRepository extends JpaRepository<AdapterEntity, Long> {
    Optional<AdapterEntity> findBySensorType(String sensorType);
    Optional<AdapterEntity> findFirstBySensorTypeOrderByCreatedAtDesc(String sensorType);
}
