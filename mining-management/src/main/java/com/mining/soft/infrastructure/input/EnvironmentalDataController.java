package com.mining.soft.infrastructure.input;

import com.mining.soft.application.api.IEnvironmentalDataServicePort;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.infrastructure.input.dto.SensorDataRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/environmental-data")
public class EnvironmentalDataController {

    private final IEnvironmentalDataServicePort servicePort;
    private final boolean cacheEvictOnWrite;

    public EnvironmentalDataController(IEnvironmentalDataServicePort servicePort,
                                       @org.springframework.beans.factory.annotation.Value("${cache.evict-on-write:true}")
                                       boolean cacheEvictOnWrite) {
        this.servicePort = servicePort;
        this.cacheEvictOnWrite = cacheEvictOnWrite;
    }

    /** Usado por la condición SpEL de @CacheEvict: invalida las cachés de lectura al ingerir. */
    public boolean isCacheEvictOnWrite() {
        return cacheEvictOnWrite;
    }

    @PostMapping
    // Ingesta: cuentas de pasarela de sensores / laboratorio (rol INGENIERO) y administradores
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INGENIERO')")
    @CacheEvict(value = {"environmental-data", "environmental-data-aggregate"}, allEntries = true,
            condition = "#root.target.cacheEvictOnWrite")
    public ResponseEntity<EnvironmentalData> processSensorData(@RequestBody SensorDataRequestDto request) {
        return new ResponseEntity<>(
                servicePort.processSensorData(request.getSensorType(), request.getPayload()),
                HttpStatus.CREATED);
    }

    @GetMapping
    // Cache only when callers request broad data (no time window or numeric filters) to avoid very high cache cardinality
    @Cacheable(value = "environmental-data", key = "'all'", condition = "#startDate==null && #endDate==null && #phMin==null && #phMax==null && #hgTotalMin==null && #alertLevel==null && #alertType==null")
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
            @RequestParam(required = false) Double redoxMin,
            @RequestParam(required = false) Double redoxMax,
            @RequestParam(required = false) Double dissolvedOxygenMin,
            @RequestParam(required = false) Double hgDisssolvedMin,
            @RequestParam(required = false) Double hgTotalMin,
            @RequestParam(required = false) Double hgSedimentMin,
            @RequestParam(required = false) Double hgFishMin,
            @RequestParam(required = false) Double methylmercuryMin,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) String alertType) {

        return ResponseEntity.ok(servicePort.getFilteredData(
                buildFilter(startDate, endDate, minLat, maxLat, minLng, maxLng,
                        phMin, phMax, turbidityMin, turbidityMax, temperatureMin, temperatureMax,
                        redoxMin, redoxMax, dissolvedOxygenMin,
                        hgDisssolvedMin, hgTotalMin, hgSedimentMin, hgFishMin, methylmercuryMin,
                        alertLevel, alertType)));
    }

    @GetMapping("/aggregate")
    // Aggregate queries are expensive; cache only when caller does not provide filters that would make the result unique per request
    @Cacheable(value = "environmental-data-aggregate", key = "'aggregate_all'", condition = "#startDate==null && #endDate==null && #minLat==null && #maxLat==null && #minLng==null && #maxLng==null && #alertLevel==null && #alertType==null")
    public ResponseEntity<EnvironmentalAggregate> getAggregateData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLng,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) String alertType) {

        return ResponseEntity.ok(servicePort.getAggregatedData(
                buildFilter(startDate, endDate, minLat, maxLat, minLng, maxLng,
                        null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null,
                        alertLevel, alertType)));
    }

    private EnvironmentalDataFilter buildFilter(
            LocalDateTime startDate, LocalDateTime endDate,
            Double minLat, Double maxLat, Double minLng, Double maxLng,
            Double phMin, Double phMax, Double turbidityMin, Double turbidityMax,
            Double temperatureMin, Double temperatureMax,
            Double redoxMin, Double redoxMax, Double dissolvedOxygenMin,
            Double hgDisssolvedMin, Double hgTotalMin, Double hgSedimentMin,
            Double hgFishMin, Double methylmercuryMin, String alertLevel, String alertType) {

        EnvironmentalDataFilter f = new EnvironmentalDataFilter();
        f.setStartDate(startDate);
        f.setEndDate(endDate);
        f.setMinLat(minLat);
        f.setMaxLat(maxLat);
        f.setMinLng(minLng);
        f.setMaxLng(maxLng);
        f.setPhMin(phMin);
        f.setPhMax(phMax);
        f.setTurbidityMin(turbidityMin);
        f.setTurbidityMax(turbidityMax);
        f.setTemperatureMin(temperatureMin);
        f.setTemperatureMax(temperatureMax);
        f.setRedoxMin(redoxMin);
        f.setRedoxMax(redoxMax);
        f.setDissolvedOxygenMin(dissolvedOxygenMin);
        f.setHgDisssolvedMin(hgDisssolvedMin);
        f.setHgTotalMin(hgTotalMin);
        f.setHgSedimentMin(hgSedimentMin);
        f.setHgFishMin(hgFishMin);
        f.setMethylmercuryMin(methylmercuryMin);
        f.setAlertLevel(alertLevel);
        f.setAlertType(alertType);
        return f;
    }
}
