package com.mining.soft.application.api;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.domain.model.Adapter;
import java.util.List;
import java.util.Map;

public interface IEnvironmentalDataServicePort {
    EnvironmentalData processSensorData(String sensorType, Map<String, Object> payload);
    List<EnvironmentalData> getFilteredData(EnvironmentalDataFilter filter);
    EnvironmentalAggregate getAggregatedData(EnvironmentalDataFilter filter);
    Adapter createAdapter(Adapter adapter);
}
