package com.mining.soft.application.service;

import com.mining.soft.domain.model.EnvironmentalData;
import com.mining.soft.infrastructure.configuration.AmbientalConfig;
import com.mining.soft.infrastructure.configuration.AmbientalConfig.Dimension;
import com.mining.soft.infrastructure.configuration.AmbientalConfig.Direccion;
import com.mining.soft.infrastructure.configuration.AmbientalConfig.Factor;
import com.mining.soft.infrastructure.configuration.AmbientalConfig.PerfilTermico;
import com.mining.soft.infrastructure.configuration.AmbientalConfig.TipoFactor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modelo de alerta v2: clasifica cada medición al momento de la ingesta.
 *
 * 1. Cada factor (parámetro + umbrales con fuente) se evalúa solo si es concluyente:
 *    valor medido y plausible, o valor censurado con LD ≤ umbral de alerta. Un proxy
 *    (cota inferior, p. ej. Hg disuelto para Hg total) solo concluye cuando supera.
 * 2. Puntaje del factor: 0, puntaje-alerta o puntaje-critico. El de la dimensión es el
 *    máximo de sus factores concluyentes.
 * 3. Puntaje compuesto = Σ wᵢ·sᵢ / Σ wᵢ sobre las dimensiones evaluadas (sin dilución
 *    por dimensiones sin datos).
 * 4. Nivel = max(nivel por puntaje, piso normativo): un factor HUMANO en CRÍTICO
 *    garantiza al menos ALERTA y cualquier superación, al menos PRECAUCION.
 * 5. Tipo: IMPACTO (supera un contaminante), RIESGO (supera una condición), MIXTO,
 *    NINGUNO (concluyente sin superaciones) o DESCONOCIDO (nada concluyente).
 */
@Service
@RequiredArgsConstructor
public class AlertScoringService {

    static final List<String> LEVELS = List.of("INDETERMINADO", "NORMAL", "PRECAUCION", "ALERTA", "CRITICO");

    private final AmbientalConfig cfg;

    public void calculateAndAssign(EnvironmentalData d) {
        PerfilTermico perfil = resolveProfile(d);
        d.setThermalProfile(perfil.name());
        boolean predador = Boolean.TRUE.equals(d.getFishPredatory());

        Map<Dimension, Double> dimScore = new EnumMap<>(Dimension.class);
        List<String> exceedances = new ArrayList<>();
        Set<String> inconclusive = new LinkedHashSet<>();
        Set<String> missing = new LinkedHashSet<>();
        int conclusive = 0;
        boolean contaminantExceeded = false;
        boolean conditionExceeded = false;
        boolean humanCritical = false;

        for (Factor f : cfg.getFactores()) {
            Evaluation e = evaluate(f, d, perfil, predador);
            switch (e.status) {
                case ABSENT -> missing.add(f.getParametro());
                case INCONCLUSIVE -> inconclusive.add(f.getId());
                case CONCLUSIVE -> {
                    conclusive++;
                    dimScore.merge(f.getDimension(), e.score, Math::max);
                    if (e.score > 0) {
                        String level = e.score >= cfg.getPuntajeCritico() ? "CRITICO" : "ALERTA";
                        exceedances.add(f.getId() + ":" + level);
                        if (f.getTipo() == TipoFactor.CONTAMINANTE) contaminantExceeded = true;
                        else conditionExceeded = true;
                        if (f.getDimension() == Dimension.HUMANA && "CRITICO".equals(level)) humanCritical = true;
                    }
                }
            }
        }

        d.setHumanRiskScore(round(dimScore.get(Dimension.HUMANA)));
        d.setEcologicalRiskScore(round(dimScore.get(Dimension.ECOLOGICA)));
        d.setGeochemicalRiskScore(round(dimScore.get(Dimension.GEOQUIMICA)));

        if (conclusive == 0) {
            d.setTotalRiskScore(null);
            d.setAlertLevel("INDETERMINADO");
            d.setAlertType("DESCONOCIDO");
            d.setLevelBasis("PUNTAJE");
        } else {
            double num = 0, den = 0;
            for (Map.Entry<Dimension, Double> en : dimScore.entrySet()) {
                double w = cfg.pesoDe(en.getKey());
                num += w * en.getValue();
                den += w;
            }
            double total = den > 0 ? num / den : 0.0;
            d.setTotalRiskScore(round(total));

            String byScore = levelFromScore(total);
            String floor = "NORMAL";
            if (humanCritical) floor = max(floor, cfg.getEscalamiento().getHumanoCritico());
            if (!exceedances.isEmpty()) floor = max(floor, cfg.getEscalamiento().getCualquierAlerta());
            String level = max(byScore, floor);
            d.setAlertLevel(level);
            d.setLevelBasis(level.equals(byScore) ? "PUNTAJE" : "NORMATIVO");
            d.setAlertType(contaminantExceeded && conditionExceeded ? "MIXTO"
                    : contaminantExceeded ? "IMPACTO"
                    : conditionExceeded ? "RIESGO" : "NINGUNO");
        }

        long totalFactors = cfg.getFactores().size();
        d.setDataCompleteness(totalFactors == 0 ? 0.0 : round((double) conclusive / totalFactors));
        d.setExceedances(exceedances.isEmpty() ? null : String.join(",", exceedances));
        d.setInconclusiveFields(inconclusive.isEmpty() ? null : String.join(",", inconclusive));
        d.setMissingFields(missing.isEmpty() ? null : String.join(",", missing));
        d.setDataSource(resolveSource(d));
    }

    // ── Evaluación de un factor ────────────────────────────────────────────
    enum Status { ABSENT, INCONCLUSIVE, CONCLUSIVE }

    record Evaluation(Status status, double score) {
        static final Evaluation ABSENT = new Evaluation(Status.ABSENT, 0);
        static final Evaluation INCONCLUSIVE = new Evaluation(Status.INCONCLUSIVE, 0);
        static Evaluation of(double s) { return new Evaluation(Status.CONCLUSIVE, s); }
    }

    Evaluation evaluate(Factor f, EnvironmentalData d, PerfilTermico perfil, boolean predador) {
        double alerta = f.alertaPara(perfil);
        double critico = f.criticoPara(predador);
        String p = f.getParametro();

        if (d.isImplausible(p)) return Evaluation.INCONCLUSIVE;

        Double v = d.valueOf(p);
        if (v != null) return Evaluation.of(score(f.getDireccion(), v, alerta, critico));

        if (d.isCensored(p)) {
            double lod = d.getCensoredLimits().get(p);
            // "< LD": el valor real es menor que LD.
            if (f.getDireccion() == Direccion.MAYOR) {
                return lod <= alerta ? Evaluation.of(0.0) : Evaluation.INCONCLUSIVE;
            }
            // Límite inferior: "< LD" con LD < crítico implica superación crítica.
            if (lod <= critico) return Evaluation.of(cfg.getPuntajeCritico());
            return lod <= alerta ? Evaluation.of(cfg.getPuntajeAlerta()) : Evaluation.INCONCLUSIVE;
        }

        if (f.getProxy() != null && !d.isImplausible(f.getProxy())) {
            Double proxy = d.valueOf(f.getProxy());
            if (proxy != null) {
                double s = score(f.getDireccion(), proxy, alerta, critico);
                // El proxy es cota inferior: solo concluye si ya supera.
                return s > 0 ? Evaluation.of(s) : Evaluation.INCONCLUSIVE;
            }
            if (d.isCensored(f.getProxy())) return Evaluation.INCONCLUSIVE;
        }
        return Evaluation.ABSENT;
    }

    private double score(Direccion dir, double v, double alerta, double critico) {
        boolean crit = dir == Direccion.MAYOR ? v > critico : v < critico;
        if (crit) return cfg.getPuntajeCritico();
        boolean alert = dir == Direccion.MAYOR ? v > alerta : v < alerta;
        return alert ? cfg.getPuntajeAlerta() : 0.0;
    }

    // ── Auxiliares ─────────────────────────────────────────────────────────
    PerfilTermico resolveProfile(EnvironmentalData d) {
        if (d.getThermalProfile() != null) {
            try { return PerfilTermico.valueOf(d.getThermalProfile().trim().toUpperCase()); }
            catch (IllegalArgumentException ignored) { /* se resuelve por altitud */ }
        }
        if (d.getAltitude() != null) {
            return d.getAltitude() >= cfg.getPerfilTermico().getAltitudFriaMin()
                    ? PerfilTermico.FRIA : PerfilTermico.CALIDA;
        }
        return cfg.getPerfilTermico().getPorDefecto();
    }

    private String levelFromScore(double total) {
        AmbientalConfig.Niveles n = cfg.getNiveles();
        if (total >= n.getCritico()) return "CRITICO";
        if (total >= n.getAlerta()) return "ALERTA";
        if (total >= n.getPrecaucion()) return "PRECAUCION";
        return "NORMAL";
    }

    static String max(String a, String b) {
        return LEVELS.indexOf(a) >= LEVELS.indexOf(b) ? a : b;
    }

    private String resolveSource(EnvironmentalData d) {
        boolean inSitu = false, lab = false;
        for (String p : EnvironmentalData.PARAMETERS) {
            if (d.valueOf(p) == null && !d.isCensored(p)) continue;
            if (cfg.getParametrosInSitu().contains(p)) inSitu = true; else lab = true;
        }
        if (inSitu && lab) return "MIXTO";
        if (lab) return "LABORATORIO";
        if (inSitu) return "SENSOR";
        return "DESCONOCIDO";
    }

    private static Double round(Double v) {
        return v == null ? null : Math.round(v * 1000.0) / 1000.0;
    }
}
