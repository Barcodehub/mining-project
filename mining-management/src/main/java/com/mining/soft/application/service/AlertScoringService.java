package com.mining.soft.application.service;

import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.infrastructure.configuration.AmbientalConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de puntuación de riesgo ambiental DEFENSIVO.
 *
 * FILOSOFÍA:
 * - Solo usa campos PRESENTES (no null)
 * - NO inventa datos faltantes
 * - Distingue entre IMPACTO (evidencia directa) y RIESGO (condiciones peligrosas)
 * - Calcula completitud de datos para transparencia
 *
 * Todos los umbrales se leen desde AmbientalConfig (config-ambiental.yaml).
 */
@Service
@RequiredArgsConstructor
public class AlertScoringService {

    private final AmbientalConfig cfg;

    /**
     * Calcula y asigna todos los scores, tipo de alerta y metadatos de calidad.
     */
    public void calculateAndAssign(EnvironmentalData d) {
        // 1. Calcular completitud de datos
        calculateDataCompleteness(d);

        // 2. Calcular scores por dimensión (solo con campos presentes)
        double human = humanRisk(d);
        double eco   = ecologicalRisk(d);
        double geo   = geochemicalRisk(d);

        d.setHumanRiskScore(round(human));
        d.setEcologicalRiskScore(round(eco));
        d.setGeochemicalRiskScore(round(geo));

        // 3. Score total ponderado
        double total = cfg.getPesosScore().getRiesgoHumano()    * human
                     + cfg.getPesosScore().getRiesgoEcologico() * eco
                     + cfg.getPesosScore().getRiesgoGeoquimico() * geo;

        d.setTotalRiskScore(round(total));

        // 4. Determinar nivel y tipo de alerta
        d.setAlertLevel(resolveLevel(total));
        d.setAlertType(resolveAlertType(d));
    }

    // ══════════════════════════════════════════════════════════════════════
    // RIESGO HUMANO (Ingesta de agua y peces)
    // ══════════════════════════════════════════════════════════════════════
    private double humanRisk(EnvironmentalData d) {
        double score = 0.0;
        int factorsEvaluated = 0;

        // Hg en peces (evidencia DIRECTA de riesgo por ingesta)
        if (d.getHgFish() != null) {
            factorsEvaluated++;
            if (d.getHgFish() >= cfg.getHgPeces().getCritico())      score = Math.max(score, 1.0);
            else if (d.getHgFish() >= cfg.getHgPeces().getAlerta())  score = Math.max(score, 0.6);
        }

        // Hg disuelto en agua (riesgo por consumo directo)
        if (d.getHgDissolved() != null) {
            factorsEvaluated++;
            if (d.getHgDissolved() >= cfg.getHgDisuelto().getCritico())     score = Math.max(score, 1.0);
            else if (d.getHgDissolved() >= cfg.getHgDisuelto().getAlerta()) score = Math.max(score, 0.5);
        }

        // Metilmercurio (forma más tóxica)
        if (d.getMethylmercury() != null) {
            factorsEvaluated++;
            if (d.getMethylmercury() >= cfg.getMetilmercurio().getCritico())     score = Math.max(score, 0.9);
            else if (d.getMethylmercury() >= cfg.getMetilmercurio().getAlerta()) score = Math.max(score, 0.5);
        }

        // Si no hay NINGÚN dato de mercurio, retornar 0 (riesgo desconocido)
        return factorsEvaluated > 0 ? score : 0.0;
    }

    // ══════════════════════════════════════════════════════════════════════
    // RIESGO ECOLÓGICO (Biota acuática y sedimento)
    // ══════════════════════════════════════════════════════════════════════
    private double ecologicalRisk(EnvironmentalData d) {
        double score = 0.0;
        int factorsEvaluated = 0;

        // Hg en sedimento (impacto en bentos)
        if (d.getHgSediment() != null) {
            factorsEvaluated++;
            if (d.getHgSediment() >= cfg.getHgSedimento().getPel())      score = Math.max(score, 1.0);
            else if (d.getHgSediment() >= cfg.getHgSedimento().getTel()) score = Math.max(score, 0.5);
        }

        // Hg total en agua (impacto en columna de agua)
        if (d.getHgTotal() != null) {
            factorsEvaluated++;
            if (d.getHgTotal() >= cfg.getHgTotal().getCritico())     score = Math.max(score, 0.8);
            else if (d.getHgTotal() >= cfg.getHgTotal().getAlerta()) score = Math.max(score, 0.4);
        }

        // Oxígeno disuelto (estrés para biota)
        if (d.getDissolvedOxygen() != null) {
            factorsEvaluated++;
            if (d.getDissolvedOxygen() <= cfg.getOxigenoDisuelto().getCritico())     score = Math.max(score, 0.7);
            else if (d.getDissolvedOxygen() <= cfg.getOxigenoDisuelto().getAlerta()) score = Math.max(score, 0.3);
        }

        return factorsEvaluated > 0 ? score : 0.0;
    }

    // ══════════════════════════════════════════════════════════════════════
    // RIESGO GEOQUÍMICO (Potencial de metilación futura - "Bomba de tiempo")
    // ══════════════════════════════════════════════════════════════════════
    private double geochemicalRisk(EnvironmentalData d) {
        double score = 0.0;

        // Condiciones críticas para metilación
        boolean phCritico  = d.getPh() != null && d.getPh() < cfg.getPh().getCriticoBajo();
        boolean redoxBajo  = d.getRedox() != null && d.getRedox() < cfg.getRedox().getCriticoBajo();
        boolean redoxAlerta = d.getRedox() != null && d.getRedox() < cfg.getRedox().getAlertaBajo();
        boolean cocAlto    = d.getOrganicCarbon() != null && d.getOrganicCarbon() > cfg.getCarbonoOrganico();

        // Combinación pH ácido + ambiente reductor = máximo riesgo de metilación
        if (phCritico && redoxBajo)          score = Math.max(score, 1.0);
        else if (phCritico || redoxBajo)     score = Math.max(score, 0.7);
        else if (redoxAlerta)                score = Math.max(score, 0.4);

        // Carbono orgánico alto amplifica el riesgo
        if (cocAlto) score = Math.min(1.0, score + 0.2);

        // Si no tenemos pH ni redox, no podemos evaluar riesgo geoquímico
        if (d.getPh() == null && d.getRedox() == null) return 0.0;

        return score;
    }

    // ══════════════════════════════════════════════════════════════════════
    // DETERMINACIÓN DE NIVEL DE ALERTA
    // ══════════════════════════════════════════════════════════════════════
    private String resolveLevel(double total) {
        if (total >= 0.75) return "CRITICO";
        if (total >= 0.50) return "ALERTA";
        if (total >= 0.25) return "PRECAUCION";
        return "NORMAL";
    }

    // ══════════════════════════════════════════════════════════════════════
    // DETERMINACIÓN DE TIPO DE ALERTA
    // ══════════════════════════════════════════════════════════════════════
    /**
     * IMPACTO: Evidencia directa de contaminación (Hg medido supera norma)
     * RIESGO: Condiciones ambientales peligrosas (pH bajo, redox bajo, etc.)
     * MIXTO: Ambos tipos presentes
     * DESCONOCIDO: Datos insuficientes para determinar
     */
    private String resolveAlertType(EnvironmentalData d) {
        boolean hasImpact = hasDirectImpact(d);
        boolean hasRisk   = hasGeochemicalRisk(d);

        if (hasImpact && hasRisk)  return "MIXTO";
        if (hasImpact)             return "IMPACTO";
        if (hasRisk)               return "RIESGO";
        return "DESCONOCIDO";
    }

    private boolean hasDirectImpact(EnvironmentalData d) {
        // Evidencia directa: mercurio medido supera umbrales
        if (d.getHgDissolved() != null && d.getHgDissolved() >= cfg.getHgDisuelto().getAlerta()) return true;
        if (d.getHgTotal() != null && d.getHgTotal() >= cfg.getHgTotal().getAlerta()) return true;
        if (d.getHgSediment() != null && d.getHgSediment() >= cfg.getHgSedimento().getTel()) return true;
        if (d.getHgFish() != null && d.getHgFish() >= cfg.getHgPeces().getAlerta()) return true;
        if (d.getMethylmercury() != null && d.getMethylmercury() >= cfg.getMetilmercurio().getAlerta()) return true;
        return false;
    }

    private boolean hasGeochemicalRisk(EnvironmentalData d) {
        // Condiciones peligrosas: ambiente propicio para metilación
        boolean phCritico = d.getPh() != null && d.getPh() < cfg.getPh().getCriticoBajo();
        boolean redoxBajo = d.getRedox() != null && d.getRedox() < cfg.getRedox().getAlertaBajo();
        boolean oxigenoBajo = d.getDissolvedOxygen() != null && d.getDissolvedOxygen() <= cfg.getOxigenoDisuelto().getAlerta();
        
        return phCritico || redoxBajo || oxigenoBajo;
    }

    // ══════════════════════════════════════════════════════════════════════
    // CÁLCULO DE COMPLETITUD DE DATOS
    // ══════════════════════════════════════════════════════════════════════
    private void calculateDataCompleteness(EnvironmentalData d) {
        int totalOptionalFields = 8; // redox, DO, COT, hgSediment, hgFish, MeHg, hgDissolved, hgTotal
        int presentFields = 0;
        List<String> missing = new ArrayList<>();

        // Campos opcionales avanzados
        if (d.getRedox() != null) presentFields++; else missing.add("redox");
        if (d.getDissolvedOxygen() != null) presentFields++; else missing.add("dissolved_oxygen");
        if (d.getOrganicCarbon() != null) presentFields++; else missing.add("organic_carbon");
        if (d.getHgSediment() != null) presentFields++; else missing.add("hg_sediment");
        if (d.getHgFish() != null) presentFields++; else missing.add("hg_fish");
        if (d.getMethylmercury() != null) presentFields++; else missing.add("methylmercury");

        // Mercurio en agua (al menos uno debe estar presente)
        if (d.getHgDissolved() != null) presentFields++;
        if (d.getHgTotal() != null) presentFields++;
        if (d.getHgDissolved() == null && d.getHgTotal() == null) {
            missing.add("hg_dissolved_OR_hg_total");
        }

        d.setDataCompleteness(round((double) presentFields / totalOptionalFields));
        d.setMissingFields(missing.isEmpty() ? null : String.join(",", missing));

        // Determinar fuente de datos
        boolean hasLabData = d.getHgSediment() != null || d.getHgFish() != null || d.getMethylmercury() != null;
        boolean hasSensorData = d.getPh() != null && d.getTemperature() != null && d.getTurbidity() != null;
        
        if (hasLabData && hasSensorData) d.setDataSource("MIXTO");
        else if (hasLabData) d.setDataSource("LABORATORIO");
        else if (hasSensorData) d.setDataSource("SENSOR");
        else d.setDataSource("DESCONOCIDO");
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
