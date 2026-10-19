package com.mining.soft.application.service;

import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.infrastructure.configuration.AmbientalConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AlertScoringService v2 con la política real (config-ambiental.yaml)")
class AlertScoringServiceTest {

    static AmbientalConfig cfg;
    static AlertScoringService scoring;

    @BeforeAll
    static void setUp() {
        cfg = PolicyFixture.load();
        scoring = new AlertScoringService(cfg);
    }

    private static EnvironmentalData base() {
        EnvironmentalData d = new EnvironmentalData();
        d.setDeviceId("T-1");
        d.setLatitude(7.3);
        d.setLongitude(-72.9);
        return d;
    }

    @Test
    @DisplayName("La política se enlaza completa: 11 factores, pesos y fuentes cargados")
    void policyBinds() {
        assertEquals(11, cfg.getFactores().size());
        assertEquals(0.45, cfg.pesoDe(AmbientalConfig.Dimension.HUMANA));
        assertEquals(20.0, cfg.getPlausibilidad().get("dissolved_oxygen").get(1));
        assertTrue(cfg.getAlertas().getNivelesPublicados().contains("CRITICO"));
    }

    @Test
    @DisplayName("Caso real La Baja (CDMB, ene-2025): CN 3.14 mg/L con Hg < LD → CRITICO por impacto")
    void laBajaCyanide() {
        EnvironmentalData d = base();
        d.setPh(8.11);
        d.setDissolvedOxygen(7.5);
        d.setCyanide(3.14);
        d.getCensoredLimits().put("hg_total", 0.5);   // "<0.0005 mg/L" = 0.5 µg/L
        scoring.calculateAndAssign(d);

        assertEquals("CRITICO", d.getAlertLevel());
        assertEquals("IMPACTO", d.getAlertType());
        assertEquals(1.0, d.getHumanRiskScore());       // CN > 0.2 mg/L (Decreto 1076)
        assertEquals(1.0, d.getEcologicalRiskScore());  // CN > CMC 0.022 mg/L (EPA)
        assertEquals(0.0, d.getGeochemicalRiskScore());
        assertTrue(d.getExceedances().contains("cn-agua-humano:CRITICO"));
        // Hg < LD con LD ≤ umbrales: evaluado sin superación (no es dato faltante)
        assertFalse(d.getExceedances().contains("hg-agua"));
    }

    @Test
    @DisplayName("Hg censurado con LD ≤ umbral es concluyente; CN censurado con LD > CCC queda no concluyente")
    void censoredSemantics() {
        EnvironmentalData d = base();
        d.setPh(7.6);
        d.getCensoredLimits().put("hg_total", 0.5);
        d.getCensoredLimits().put("cyanide", 0.025);
        scoring.calculateAndAssign(d);

        assertEquals("NORMAL", d.getAlertLevel());
        assertEquals("NINGUNO", d.getAlertType());
        assertNotNull(d.getInconclusiveFields());
        assertTrue(d.getInconclusiveFields().contains("cn-agua-ecologico"));
        assertFalse(d.getInconclusiveFields().contains("cn-agua-humano"));
        assertFalse(d.getInconclusiveFields().contains("hg-agua-humano"));
    }

    @Test
    @DisplayName("Superación estricta: Hg = 1.0 µg/L no supera el límite de 1.0")
    void strictExceedance() {
        EnvironmentalData d = base();
        d.setHgTotal(1.0);
        scoring.calculateAndAssign(d);
        assertFalse(String.valueOf(d.getExceedances()).contains("hg-agua-humano"));
        // 1.0 > CCC 0.77 sí supera el criterio ecológico
        assertTrue(d.getExceedances().contains("hg-agua-ecologico:ALERTA"));
    }

    @Test
    @DisplayName("Sin dilución: una sola dimensión evaluable no se promedia con ceros")
    void noDilution() {
        EnvironmentalData d = base();
        d.setHgTotal(2.5);          // > 2.0 humano y > 1.4 ecológico
        scoring.calculateAndAssign(d);
        assertNull(d.getGeochemicalRiskScore());       // sin pH ni redox: no evaluada
        assertEquals(1.0, d.getTotalRiskScore());
        assertEquals("CRITICO", d.getAlertLevel());
    }

    @Test
    @DisplayName("Piso normativo: As > 0.05 mg/L (CRÍTICO humano) garantiza al menos ALERTA")
    void normativeFloor() {
        EnvironmentalData d = base();
        d.setArsenic(0.06);
        d.setPh(7.5);
        d.setDissolvedOxygen(8.0);
        scoring.calculateAndAssign(d);
        // puntaje = 0.45·1 / (0.45+0.35+0.20) = 0.45 → PRECAUCION; el piso lo eleva a ALERTA
        assertEquals(0.45, d.getTotalRiskScore());
        assertEquals("ALERTA", d.getAlertLevel());
        assertEquals("NORMATIVO", d.getLevelBasis());
    }

    @Test
    @DisplayName("Condiciones sin contaminante (pH 4.5, OD 1.5) → RIESGO")
    void riskWithoutContaminant() {
        EnvironmentalData d = base();
        d.setPh(4.5);
        d.setDissolvedOxygen(1.5);
        scoring.calculateAndAssign(d);
        assertEquals("RIESGO", d.getAlertType());
        assertEquals("CRITICO", d.getAlertLevel());
        assertEquals("SENSOR", d.getDataSource());
    }

    @Test
    @DisplayName("Perfil térmico por altitud: OD 4.5 mg/L es ALERTA en agua fría (≥ 2000 m) y no en cálida")
    void thermalProfile() {
        EnvironmentalData cold = base();
        cold.setAltitude(2700.0);
        cold.setDissolvedOxygen(4.5);
        scoring.calculateAndAssign(cold);
        assertEquals("FRIA", cold.getThermalProfile());
        assertTrue(cold.getExceedances().contains("oxigeno-disuelto:ALERTA"));

        EnvironmentalData warm = base();
        warm.setAltitude(300.0);
        warm.setDissolvedOxygen(4.5);
        scoring.calculateAndAssign(warm);
        assertEquals("CALIDA", warm.getThermalProfile());
        assertNull(warm.getExceedances());
    }

    @Test
    @DisplayName("Pescado: 0.7 mg/kg es CRÍTICO en no predadoras y ALERTA en predadoras (Res. 122/2012)")
    void fishPredatory() {
        EnvironmentalData a = base();
        a.setHgFish(0.7);
        scoring.calculateAndAssign(a);
        assertTrue(a.getExceedances().contains("hg-pescado:CRITICO"));

        EnvironmentalData b = base();
        b.setHgFish(0.7);
        b.setFishPredatory(true);
        scoring.calculateAndAssign(b);
        assertTrue(b.getExceedances().contains("hg-pescado:ALERTA"));
    }

    @Test
    @DisplayName("Proxy: Hg disuelto concluye solo si supera; si no supera, Hg total queda no concluyente")
    void dissolvedProxy() {
        EnvironmentalData hi = base();
        hi.setHgDissolved(1.5);
        scoring.calculateAndAssign(hi);
        assertTrue(hi.getExceedances().contains("hg-agua-humano:ALERTA"));

        EnvironmentalData lo = base();
        lo.setHgDissolved(0.3);
        lo.setPh(7.0);
        scoring.calculateAndAssign(lo);
        assertTrue(lo.getInconclusiveFields().contains("hg-agua-humano"));
    }

    @Test
    @DisplayName("Valor implausible (OD 419 mg/L) no se usa y la medición queda indeterminada si no hay más")
    void implausible() {
        EnvironmentalData d = base();
        d.getImplausibleParameters().add("dissolved_oxygen");
        scoring.calculateAndAssign(d);
        assertEquals("INDETERMINADO", d.getAlertLevel());
        assertEquals("DESCONOCIDO", d.getAlertType());
        assertNull(d.getTotalRiskScore());
        assertTrue(d.getInconclusiveFields().contains("oxigeno-disuelto"));
    }

    @Test
    @DisplayName("Fuente de datos: Hg en agua es análisis de laboratorio")
    void dataSource() {
        EnvironmentalData d = base();
        d.setPh(7.2);
        d.setHgTotal(0.4);
        scoring.calculateAndAssign(d);
        assertEquals("MIXTO", d.getDataSource());
    }
}
