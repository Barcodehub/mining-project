package com.mining.soft.infrastructure.out.jpa.adapter;

import com.mining.soft.application.spi.IEnvironmentalDataPersistencePort;
import com.mining.soft.domain.model.Adapter;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.infrastructure.out.jpa.mapper.IEnvironmentalEntityMapper;
import com.mining.soft.infrastructure.out.jpa.repository.IAdapterRepository;
import com.mining.soft.infrastructure.out.jpa.repository.IEnvironmentalDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class EnvironmentalDataJpaAdapter implements IEnvironmentalDataPersistencePort {

    private final IEnvironmentalDataRepository dataRepository;
    private final IAdapterRepository adapterRepository;
    private final IEnvironmentalEntityMapper entityMapper;

    @Override
    @CacheEvict(value = {"environmental-data", "environmental-data-aggregate"}, allEntries = true)
    public EnvironmentalData saveEnvironmentalData(EnvironmentalData data) {
        return entityMapper.toDataModel(dataRepository.save(entityMapper.toDataEntity(data)));
    }

    @Override
    public List<EnvironmentalData> findFiltered(EnvironmentalDataFilter filter) {
        return entityMapper.toDataModelList(dataRepository.findFiltered(filter));
    }

    @Override
    public EnvironmentalAggregate getAggregated(EnvironmentalDataFilter filter) {
        return dataRepository.getAggregated(filter);
    }

    @Override
    @CacheEvict(value = "adapters", allEntries = true)
    public Adapter saveAdapter(Adapter adapter) {
        return entityMapper.toAdapterModel(adapterRepository.save(entityMapper.toAdapterEntity(adapter)));
    }

    @Override
    @Cacheable(value = "adapters", key = "#sensorType")
    public Optional<Adapter> findAdapterBySensorType(String sensorType) {
        return adapterRepository.findBySensorType(sensorType).map(entityMapper::toAdapterModel);
    }
}
