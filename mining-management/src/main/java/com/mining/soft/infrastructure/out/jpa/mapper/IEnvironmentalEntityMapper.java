package com.mining.soft.infrastructure.out.jpa.mapper;

import com.mining.soft.domain.model.Adapter;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.infrastructure.out.jpa.entity.AdapterEntity;
import com.mining.soft.infrastructure.out.jpa.entity.EnvironmentalDataEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IEnvironmentalEntityMapper {
    AdapterEntity toAdapterEntity(Adapter adapter);
    Adapter toAdapterModel(AdapterEntity adapterEntity);

    EnvironmentalDataEntity toDataEntity(EnvironmentalData data);
    EnvironmentalData toDataModel(EnvironmentalDataEntity dataEntity);

    List<EnvironmentalData> toDataModelList(List<EnvironmentalDataEntity> entities);
}
