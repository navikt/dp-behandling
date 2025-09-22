CREATE INDEX ON meldekort (ident, fom, løpenummer DESC)
    WHERE behandling_ferdig IS NULL
        AND korrigert_av_meldekort_id IS NULL
        AND satt_på_vent IS NULL;
