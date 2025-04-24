ALTER TABLE meldekort
    ADD COLUMN korrigert_av_meldekort_id BIGINT REFERENCES meldekort (meldekort_id);

-- Legg til indeks på korrigert_av_meldekort_id
CREATE INDEX meldekort_korrigert_av_meldekort_id_idx ON meldekort (korrigert_av_meldekort_id) WHERE korrigert_av_meldekort_id IS NULL;