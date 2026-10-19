-- Añadir columna cluster_id a tabla posts para analytics por cluster
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS cluster_id VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_post_cluster ON posts(cluster_id);

-- Opcional: intentar poblar cluster_id desde campos geo (si existe lógica previa)
-- UPDATE posts SET cluster_id = 'UNKNOWN' WHERE cluster_id IS NULL;

