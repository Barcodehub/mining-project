package com.mining.soft.application.usecase;

import com.mining.soft.application.service.AlertScoringService;
import com.mining.soft.application.service.PolicyFixture;
import com.mining.soft.application.spi.IAlertEventPort;
import com.mining.soft.application.spi.IEnvironmentalDataPersistencePort;
import com.mining.soft.domain.model.Adapter;
import com.mining.soft.domain.model.EnvironmentalAggregate;
import com.mining.soft.domain.model.EnvironmentalAlertEvent;
import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.domain.model.EnvironmentalDataFilter;
import com.mining.soft.infrastructure.configuration.AmbientalConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EnvironmentalDataUseCase v2: ingesta heterogénea, censurados, plausibilidad y publicación")
class EnvironmentalDataUseCaseTest {

    /** Persistencia en memoria para aislar el caso de uso. */
    static class InMemoryPersistence implements IEnvironmentalDataPersistencePort {
        final AtomicLong ids = new AtomicLong();
        final Map<String, Adapter> adapters = new HashMap<>();
        final List<EnvironmentalData> saved = new ArrayList<>();
        public EnvironmentalData saveEnvironmentalData(EnvironmentalData d) { d.setId(ids.incrementAndGet()); saved.add(d); return d; }
        public List<EnvironmentalData> findFiltered(EnvironmentalDataFilter f) { return saved; }
        public EnvironmentalAggregate getAggregated(EnvironmentalDataFilter f) { return new EnvironmentalAggregate(); }
        public Adapter saveAdapter(Adapter a) { adapters.put(a.getSensorType(), a); return a; }
        public Optional<Adapter> findAdapterBySensorType(String t) { return Optional.ofNullable(adapters.get(t)); }
    }

    InMemoryPersistence persistence;
    List<EnvironmentalAlertEvent> published;
    EnvironmentalDataUseCase useCase;

    @BeforeEach
    void setUp() {
        AmbientalConfig cfg = PolicyFixture.load();
        persistence = new InMemoryPersistence();
        published = new ArrayList<>();
        IAlertEventPort port = published::add;
        useCase = new EnvironmentalDataUseCase(persistence, new AlertScoringService(cfg), port, cfg);
    }

    private void registerCdmbAdapter() {
        Adapter a = new Adapter();
        a.setSensorType("cdmb_open_data");
        a.setMappingJson("""
                {"device_id":"c_digo_cdmb","lat":"lat","lng":"lon","altitude":"altura",
                 "timestamp":"fecha_y_hora_de_monitoreo","ph":"ph_unidades_de_ph",
                 "dissolved_oxygen":"oxigeno_disuelto_mg_l","turbidity":"turbiedad_ntu",
                 "temperature":"t_muestra_c",
                 "hg_total":{"path":"mercurio_mg_l","factor":1000},
                 "cyanide":"cianuro_mg_l","arsenic":"arsenico_mg_l"}""");
        useCase.createAdapter(a);
    }

    private static Map<String, Object> cdmbRow(String hg, String cn, String od) {
        Map<String, Object> r = new HashMap<>();
        r.put("c_digo_cdmb", "QLB-01");
        r.put("lat", 7.3667);
        r.put("lon", -72.8917);
        r.put("altura", "2669");
        r.put("fecha_y_hora_de_monitoreo", "2025-01-13T10:00:00.000");
        r.put("ph_unidades_de_ph", "8.11");
        r.put("oxigeno_disuelto_mg_l", od);
        r.put("turbiedad_ntu", "70");
        r.put("t_muestra_c", "14.2");
        r.put("mercurio_mg_l", hg);
        r.put("cianuro_mg_l", cn);
        r.put("arsenico_mg_l", "-999");
        return r;
    }

    @Test
    @DisplayName("Fila real de la CDMB (La Baja): Hg '<0.0005' ya no se rechaza; CN 3.14 → CRITICO y se publica")
    void cdmbRealRowIsAcceptedAndPublished() {
        registerCdmbAdapter();
        EnvironmentalData d = useCase.processSensorData("cdmb_open_data", cdmbRow("<0.0005", "3.14", "7.5"));

        assertNotNull(d.getId());
        assertNull(d.getHgTotal());
        assertEquals("hg_total<0.5", d.getCensoredFields());     // mg/L → µg/L por el factor del adapter
        assertNull(d.getArsenic());                               // -999 = no analizado
        assertEquals("FRIA", d.getThermalProfile());              // 2669 m
        assertEquals("CRITICO", d.getAlertLevel());
        assertEquals(1, published.size());
        EnvironmentalAlertEvent e = published.get(0);
        assertEquals(d.getId(), e.measurementId());
        assertTrue(e.ingestedAtEpochMs() > 0);
        assertTrue(e.publishedAtEpochMs() >= e.ingestedAtEpochMs());
    }

    @Test
    @DisplayName("Valor implausible (OD 419 mg/L): no se guarda en su columna, se marca y no se usa")
    void implausibleValueIsFlagged() {
        registerCdmbAdapter();
        EnvironmentalData d = useCase.processSensorData("cdmb_open_data", cdmbRow("0.0003", "<0.025", "419"));
        assertNull(d.getDissolvedOxygen());
        assertTrue(d.getQualityFlags().contains("IMPLAUSIBLE:dissolved_oxygen=419"));
        assertEquals(0.3, d.getHgTotal(), 1e-9);
        assertEquals("NORMAL", d.getAlertLevel());
        assertTrue(published.isEmpty(), "NORMAL no se publica");
    }

    @Test
    @DisplayName("Sonda in situ sin mercurio (solo pH/OD) es válida: el v1 la rechazaba")
    void sensorOnlyPayloadIsValid() {
        Map<String, Object> p = new HashMap<>();
        p.put("deviceId", "SONDA-1");
        p.put("latitude", 8.07);
        p.put("longitude", -72.82);
        p.put("ph", 7.1);
        p.put("dissolved_oxygen", 6.2);
        p.put("timestamp", "2026-04-19T14:32:00Z");
        EnvironmentalData d = useCase.processSensorData("multi_parameter_sensor", p);
        assertEquals("SENSOR", d.getDataSource());
        assertEquals("NORMAL", d.getAlertLevel());
        assertEquals(14, d.getTimestamp().getHour());             // Z → UTC
    }

    @Test
    @DisplayName("Sin parámetros reconocidos o sin ubicación → 400")
    void validation() {
        Map<String, Object> p = new HashMap<>();
        p.put("deviceId", "X");
        p.put("latitude", 8.0);
        p.put("longitude", -72.0);
        assertThrows(IllegalArgumentException.class, () -> useCase.processSensorData("t", p));

        Map<String, Object> q = new HashMap<>(p);
        q.put("ph", 7.0);
        q.put("latitude", 45.0);                                  // fuera de Colombia → implausible
        assertThrows(IllegalArgumentException.class, () -> useCase.processSensorData("t", q));
    }

    @Test
    @DisplayName("Mapeo inválido del adapter se rechaza al crearlo")
    void invalidAdapterMapping() {
        Adapter a = new Adapter();
        a.setSensorType("bad");
        a.setMappingJson("{\"hg_total\": 42}");
        assertThrows(IllegalArgumentException.class, () -> useCase.createAdapter(a));
    }
}
