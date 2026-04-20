package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.infrastructure.out.jpa.entity.EnvironmentalDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IEnvironmentalDataRepository extends JpaRepository<EnvironmentalDataEntity, Long>, EnvironmentalDataRepositoryCustom {
}
