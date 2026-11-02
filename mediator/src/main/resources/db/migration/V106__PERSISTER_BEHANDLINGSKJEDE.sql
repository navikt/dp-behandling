ALTER TABLE behandling
    ADD COLUMN behandlingskjede_id uuid REFERENCES behandling (behandling_id);

WITH RECURSIVE kjede AS (
    SELECT behandling_id, behandling_id AS behandlingskjede_id
    FROM behandling
    WHERE basert_på_behandling_id IS NULL

    UNION ALL

    SELECT b.behandling_id, k.behandlingskjede_id
    FROM behandling b
    JOIN kjede k ON b.basert_på_behandling_id = k.behandling_id
)
UPDATE behandling b
SET behandlingskjede_id = k.behandlingskjede_id
FROM kjede k
WHERE b.behandling_id = k.behandling_id;

ALTER TABLE behandling
    ALTER COLUMN behandlingskjede_id SET NOT NULL;

CREATE INDEX idx_behandling_behandlingskjede_id
    ON behandling (behandlingskjede_id);
