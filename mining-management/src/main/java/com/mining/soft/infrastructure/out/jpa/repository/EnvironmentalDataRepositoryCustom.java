package com.mining.soft.infrastructure.out.jpa.repository;

import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.infrastructure.out.jpa.entity.EnvironmentalDataEntity;
import java.util.List;

public interface EnvironmentalDataRepositoryCustom {
    List<EnvironmentalDataEntity> findFiltered(EnvironmentalDataFilter filter);
    EnvironmentalAggregate getAggregated(EnvironmentalDataFilter filter);
}
