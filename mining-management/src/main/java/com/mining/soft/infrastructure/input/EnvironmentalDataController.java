package com.mining.soft.infrastructure.input;

import com.mining.soft.application.api.IEnvironmentalDataServicePort;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.infrastructure.input.dto.SensorDataRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/environmental-data")
@RequiredArgsConstructor
public class EnvironmentalDataController {

    private final IEnvironmentalDataServicePort servicePort;

    @PostMapping
    @CacheEvict(value = "environmental-data", allEntries = true)
    public ResponseEntity<EnvironmentalData> processSensorData(@RequestBody SensorDataRequestDto request) {
        return new ResponseEntity<>(servicePort.processSensorData(request.getSensorType(), request.getPayload()), HttpStatus.CREATED);
    }

    @GetMapping
    @Cacheable(value = "environmental-data", key = "{#startDate, #endDate, #minLat, #maxLat, #minLng, #maxLng, #phMin, #phMax, #turbidityMin, #turbidityMax, #temperatureMin, #temperatureMax, #mercuryMin, #mercuryMax}")
    public ResponseEntity<List<EnvironmentalData>> getEnvironmentalData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) Double phMin,
            @RequestParam(required = false) Double phMax,
            @RequestParam(required = false) Double turbidityMin,
            @RequestParam(required = false) Double turbidityMax,
            @RequestParam(required = false) Double temperatureMin,
            @RequestParam(required = false) Double temperatureMax,
            @RequestParam(required = false) Double mercuryMin,
            @RequestParam(required = false) Double mercuryMax) {

        EnvironmentalDataFilter filter = buildFilter(startDate, endDate, minLat, maxLat, minLng, maxLng,
                phMin, phMax, turbidityMin, turbidityMax, temperatureMin, temperatureMax, mercuryMin, mercuryMax);

        return ResponseEntity.ok(servicePort.getFilteredData(filter));
    }

    @GetMapping("/aggregate")
    @Cacheable(value = "environmental-data-aggregate", key = "{#startDate, #endDate, #minLat, #maxLat, #minLng, #maxLng, #phMin, #phMax, #turbidityMin, #turbidityMax, #temperatureMin, #temperatureMax, #mercuryMin, #mercuryMax}")
    public ResponseEntity<EnvironmentalAggregate> getAggregateData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) Double phMin,
            @RequestParam(required = false) Double phMax,
            @RequestParam(required = false) Double turbidityMin,
            @RequestParam(required = false) Double turbidityMax,
            @RequestParam(required = false) Double temperatureMin,
            @RequestParam(required = false) Double temperatureMax,
            @RequestParam(required = false) Double mercuryMin,
            @RequestParam(required = false) Double mercuryMax) {

        EnvironmentalDataFilter filter = buildFilter(startDate, endDate, minLat, maxLat, minLng, maxLng,
                phMin, phMax, turbidityMin, turbidityMax, temperatureMin, temperatureMax, mercuryMin, mercuryMax);

        return ResponseEntity.ok(servicePort.getAggregatedData(filter));
    }

    private EnvironmentalDataFilter buildFilter(
            LocalDateTime startDate, LocalDateTime endDate, Double minLat, Double maxLat, Double minLng, Double maxLng,
            Double phMin, Double phMax, Double turbidityMin, Double turbidityMax, Double temperatureMin, Double temperatureMax,
            Double mercuryMin, Double mercuryMax) {

        EnvironmentalDataFilter filter = new EnvironmentalDataFilter();
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setMinLat(minLat);
        filter.setMaxLat(maxLat);
        filter.setMinLng(minLng);
        filter.setMaxLng(maxLng);
        filter.setPhMin(phMin);
        filter.setPhMax(phMax);
        filter.setTurbidityMin(turbidityMin);
        filter.setTurbidityMax(turbidityMax);
        filter.setTemperatureMin(temperatureMin);
        filter.setTemperatureMax(temperatureMax);
        filter.setMercuryMin(mercuryMin);
        filter.setMercuryMax(mercuryMax);
        return filter;
    }
}
