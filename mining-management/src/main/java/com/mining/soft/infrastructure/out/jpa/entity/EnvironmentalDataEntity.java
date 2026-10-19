package com.mining.soft.infrastructure.out.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "environmental_data", indexes = {
    @Index(name = "idx_env_timestamp",    columnList = "timestamp"),
    @Index(name = "idx_env_lat_lng",      columnList = "latitude, longitude"),
    @Index(name = "idx_env_cluster",      columnList = "cluster_id"),
    @Index(name = "idx_env_hg_dissolved", columnList = "hg_dissolved"),
    @Index(name = "idx_env_hg_total",     columnList = "hg_total"),
    @Index(name = "idx_env_hg_sediment",  columnList = "hg_sediment"),
    @Index(name = "idx_env_hg_fish",      columnList = "hg_fish"),
    @Index(name = "idx_env_methylhg",     columnList = "methylmercury"),
    @Index(name = "idx_env_alert_level",  columnList = "alert_level"),
    @Index(name = "idx_env_alert_type",   columnList = "alert_type"),
    @Index(name = "idx_env_ph",           columnList = "ph"),
    @Index(name = "idx_env_redox",        columnList = "redox"),
    @Index(name = "idx_env_data_source",  columnList = "data_source"),
    @Index(name = "idx_env_cyanide",      columnList = "cyanide")
})
@Getter @Setter
public class EnvironmentalDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "cluster_id")
    private String clusterId;

    // ── Fisicoquímicas básicas (OBLIGATORIAS) ──────────────────────────────
    private Double altitude;

    @Column(name = "thermal_profile", length = 10)
    private String thermalProfile;

    @Column(name = "fish_predatory")
    private Boolean fishPredatory;

    // ── Fisicoquímicas in situ (opcionales: se guarda solo lo medido) ──────
    private Double ph;

    private Double turbidity;

    private Double temperature;

    // ── Geoquímicas avanzadas (OPCIONALES) ─────────────────────────────────
    private Double redox;

    @Column(name = "dissolved_oxygen")
    private Double dissolvedOxygen;

    @Column(name = "organic_carbon")
    private Double organicCarbon;

    // ── Mercurio fraccionado (AL MENOS UNO OBLIGATORIO) ────────────────────
    @Column(name = "hg_dissolved")
    private Double hgDissolved;

    @Column(name = "hg_total")
    private Double hgTotal;

    // ── Mercurio en otros compartimentos (OPCIONALES) ──────────────────────
    @Column(name = "hg_sediment")
    private Double hgSediment;

    @Column(name = "hg_fish")
    private Double hgFish;

    @Column(name = "methylmercury")
    private Double methylmercury;

    private Double cyanide;

    private Double arsenic;

    // ── Scores de riesgo ────────────────────────────────────────────────────
    @Column(name = "human_risk_score")
    private Double humanRiskScore;

    @Column(name = "ecological_risk_score")
    private Double ecologicalRiskScore;

    @Column(name = "geochemical_risk_score")
    private Double geochemicalRiskScore;

    @Column(name = "total_risk_score")
    private Double totalRiskScore;

    @Column(name = "alert_level", length = 20)
    private String alertLevel;

    @Column(name = "alert_type", length = 20)
    private String alertType;

    @Column(name = "level_basis", length = 20)
    private String levelBasis;

    @Column(length = 500)
    private String exceedances;

    // ── Metadatos de calidad de datos ──────────────────────────────────────
    @Column(name = "data_completeness")
    private Double dataCompleteness;

    @Column(name = "missing_fields", length = 500)
    private String missingFields;

    @Column(name = "censored_fields", length = 500)
    private String censoredFields;

    @Column(name = "inconclusive_fields", length = 500)
    private String inconclusiveFields;

    @Column(name = "quality_flags", length = 1000)
    private String qualityFlags;

    @Column(name = "data_source", length = 20)
    private String dataSource;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ingested_at")
    private LocalDateTime ingestedAt;
}
