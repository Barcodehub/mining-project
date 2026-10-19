-- ─────────────────────────────────────────────────────────────────────────────
-- MIGRACIÓN: environmental_data → modelo científico de mercurio fraccionado
-- Compatibilidad hacia atrás: columna legacy 'mercury' se mapea a hg_total
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Agregar columnas nuevas (nullable para compatibilidad con datos existentes)
ALTER TABLE environmental_data
    ADD COLUMN IF NOT EXISTS cluster_id          VARCHAR(20),
    ADD COLUMN IF NOT EXISTS redox               DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS dissolved_oxygen    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS organic_carbon      DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS hg_dissolved        DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS hg_total            DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS hg_sediment         DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS hg_fish             DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS methylmercury       DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS human_risk_score    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS ecological_risk_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS geochemical_risk_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS total_risk_score    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS alert_level         VARCHAR(20);

-- 2. Migrar datos legacy: columna 'mercury' (ppm) → hg_total (µg/L)
--    Conversión: 1 ppm = 1000 µg/L (para agua)
UPDATE environmental_data
SET hg_total = mercury * 1000.0
WHERE mercury IS NOT NULL AND hg_total IS NULL;

-- 3. Estimación conservadora de hg_dissolved desde legacy (70% del total en agua)
UPDATE environmental_data
SET hg_dissolved = hg_total * 0.70
WHERE hg_total IS NOT NULL AND hg_dissolved IS NULL;

-- 4. Asignar nivel de alerta básico a registros legacy (sin scores completos)
UPDATE environmental_data
SET alert_level = CASE
    WHEN hg_total >= 2.0 THEN 'CRITICO'
    WHEN hg_total >= 1.0 THEN 'ALERTA'
    WHEN hg_total >= 0.5 THEN 'PRECAUCION'
    ELSE 'NORMAL'
END
WHERE alert_level IS NULL;

-- 5. Nuevos índices para consultas de rendimiento
CREATE INDEX IF NOT EXISTS idx_env_cluster      ON environmental_data(cluster_id);
CREATE INDEX IF NOT EXISTS idx_env_hg_dissolved ON environmental_data(hg_dissolved);
CREATE INDEX IF NOT EXISTS idx_env_hg_sediment  ON environmental_data(hg_sediment);
CREATE INDEX IF NOT EXISTS idx_env_hg_fish      ON environmental_data(hg_fish);
CREATE INDEX IF NOT EXISTS idx_env_methylhg     ON environmental_data(methylmercury);
CREATE INDEX IF NOT EXISTS idx_env_alert_level  ON environmental_data(alert_level);
CREATE INDEX IF NOT EXISTS idx_env_redox        ON environmental_data(redox);

-- 6. Tabla de configuración de parámetros ambientales (alternativa a YAML)
CREATE TABLE IF NOT EXISTS config_parametros (
    id              BIGSERIAL PRIMARY KEY,
    region          VARCHAR(100) NOT NULL,
    parametro       VARCHAR(100) NOT NULL,
    valor_alerta    DOUBLE PRECISION,
    valor_critico   DOUBLE PRECISION,
    unidad          VARCHAR(30),
    descripcion     TEXT,
    activo          BOOLEAN DEFAULT TRUE,
    updated_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE (region, parametro)
);

-- Datos iniciales para Norte de Santander
INSERT INTO config_parametros (region, parametro, valor_alerta, valor_critico, unidad, descripcion) VALUES
('Norte de Santander', 'hg_dissolved',   1.0,  2.0,  'µg/L',   'Mercurio disuelto en agua - OMS'),
('Norte de Santander', 'hg_total',       1.0,  2.0,  'µg/L',   'Mercurio total en agua - IDEAM'),
('Norte de Santander', 'methylmercury',  0.5,  1.0,  'µg/L',   'Metilmercurio - EPA criterion'),
('Norte de Santander', 'hg_sediment_tel',0.17, 0.48, 'mg/kg',  'Hg sedimento TEL/PEL - CCME'),
('Norte de Santander', 'hg_fish',        0.3,  0.5,  'mg/kg',  'Hg en peces - FDA/OMS'),
('Norte de Santander', 'ph_min',         6.5,  5.5,  'unidades','pH mínimo ecosistemas acuáticos'),
('Norte de Santander', 'redox_critico',  100,  0,    'mV',     'Potencial redox - metilación activa'),
('Norte de Santander', 'dissolved_oxygen',4.0, 2.0,  'mg/L',   'Oxígeno disuelto - hipoxia'),
('Norte de Santander', 'turbidity',      50.0, 100.0,'NTU',    'Turbidez - IDEAM')
ON CONFLICT (region, parametro) DO NOTHING;
