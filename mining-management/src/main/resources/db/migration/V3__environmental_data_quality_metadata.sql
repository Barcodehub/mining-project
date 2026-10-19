-- ─────────────────────────────────────────────────────────────────────────────
-- MIGRACIÓN V3: Agregar metadatos de calidad de datos y tipo de alerta
-- Filosofía: Sistema honesto que no inventa datos
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Agregar columnas de tipo de alerta y calidad de datos
ALTER TABLE environmental_data
    ADD COLUMN IF NOT EXISTS alert_type         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS data_completeness  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS missing_fields     VARCHAR(500),
    ADD COLUMN IF NOT EXISTS data_source        VARCHAR(20);

-- 2. Actualizar registros existentes con valores por defecto
UPDATE environmental_data
SET alert_type = 'DESCONOCIDO',
    data_source = 'SENSOR',
    data_completeness = 0.5
WHERE alert_type IS NULL;

-- 3. Recalcular tipo de alerta para registros legacy
UPDATE environmental_data
SET alert_type = CASE
    -- IMPACTO: Evidencia directa de contaminación
    WHEN (hg_dissolved >= 1.0 OR hg_total >= 1.0 OR hg_sediment >= 0.17 OR hg_fish >= 0.3) THEN 'IMPACTO'
    -- RIESGO: Condiciones peligrosas
    WHEN (ph < 5.5 OR redox < 100 OR dissolved_oxygen < 4.0) THEN 'RIESGO'
    -- MIXTO: Ambos
    WHEN (hg_dissolved >= 1.0 OR hg_total >= 1.0) AND (ph < 5.5 OR redox < 100) THEN 'MIXTO'
    ELSE 'DESCONOCIDO'
END
WHERE alert_type = 'DESCONOCIDO';

-- 4. Calcular completitud de datos para registros existentes
UPDATE environmental_data
SET data_completeness = (
    (CASE WHEN redox IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN dissolved_oxygen IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN organic_carbon IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN hg_sediment IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN hg_fish IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN methylmercury IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN hg_dissolved IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN hg_total IS NOT NULL THEN 1 ELSE 0 END)::DOUBLE PRECISION / 8.0
)
WHERE data_completeness IS NULL OR data_completeness = 0.5;

-- 5. Identificar campos faltantes
UPDATE environmental_data
SET missing_fields = CONCAT_WS(',',
    CASE WHEN redox IS NULL THEN 'redox' END,
    CASE WHEN dissolved_oxygen IS NULL THEN 'dissolved_oxygen' END,
    CASE WHEN organic_carbon IS NULL THEN 'organic_carbon' END,
    CASE WHEN hg_sediment IS NULL THEN 'hg_sediment' END,
    CASE WHEN hg_fish IS NULL THEN 'hg_fish' END,
    CASE WHEN methylmercury IS NULL THEN 'methylmercury' END,
    CASE WHEN hg_dissolved IS NULL AND hg_total IS NULL THEN 'hg_dissolved_OR_hg_total' END
)
WHERE missing_fields IS NULL;

-- 6. Limpiar campos vacíos
UPDATE environmental_data
SET missing_fields = NULL
WHERE missing_fields = '' OR missing_fields = ',';

-- 7. Determinar fuente de datos
UPDATE environmental_data
SET data_source = CASE
    WHEN (hg_sediment IS NOT NULL OR hg_fish IS NOT NULL OR methylmercury IS NOT NULL) 
         AND (ph IS NOT NULL AND temperature IS NOT NULL AND turbidity IS NOT NULL) THEN 'MIXTO'
    WHEN (hg_sediment IS NOT NULL OR hg_fish IS NOT NULL OR methylmercury IS NOT NULL) THEN 'LABORATORIO'
    WHEN (ph IS NOT NULL AND temperature IS NOT NULL AND turbidity IS NOT NULL) THEN 'SENSOR'
    ELSE 'DESCONOCIDO'
END
WHERE data_source IS NULL OR data_source = 'SENSOR';

-- 8. Nuevos índices para consultas de calidad
CREATE INDEX IF NOT EXISTS idx_env_alert_type   ON environmental_data(alert_type);
CREATE INDEX IF NOT EXISTS idx_env_data_source  ON environmental_data(data_source);
CREATE INDEX IF NOT EXISTS idx_env_completeness ON environmental_data(data_completeness);

-- 9. Comentarios en columnas para documentación
COMMENT ON COLUMN environmental_data.alert_type IS 'IMPACTO (evidencia directa) | RIESGO (condiciones peligrosas) | MIXTO | DESCONOCIDO';
COMMENT ON COLUMN environmental_data.data_completeness IS 'Porcentaje de campos opcionales presentes (0.0 - 1.0)';
COMMENT ON COLUMN environmental_data.missing_fields IS 'Lista CSV de campos críticos faltantes';
COMMENT ON COLUMN environmental_data.data_source IS 'SENSOR | LABORATORIO | MIXTO | DESCONOCIDO';
