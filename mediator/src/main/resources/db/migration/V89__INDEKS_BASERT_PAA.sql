CREATE INDEX idx_behandling_basert_pa
    ON behandling (basert_på_behandling_id)
    WHERE basert_på_behandling_id IS NOT NULL;