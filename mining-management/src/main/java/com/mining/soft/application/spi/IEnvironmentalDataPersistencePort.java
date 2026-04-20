package com.mining.soft.application.spi;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.domain.model.Adapter;
import java.util.List;
import java.util.Optional;

public interface IEnvironmentalDataPersistencePort {
    EnvironmentalData saveEnvironmentalData(EnvironmentalData data);
    List<EnvironmentalData> findFiltered(EnvironmentalDataFilter filter);
    EnvironmentalAggregate getAggregated(EnvironmentalDataFilter filter);
    
    Adapter saveAdapter(Adapter adapter);
    Optional<Adapter> findAdapterBySensorType(String sensorType);
}
