-- Steg 1: Dropp eksisterende fremmednøkkel
ALTER TABLE meldekort
    DROP CONSTRAINT meldekort_korrigert_av_meldekort_id_fkey;
ALTER TABLE meldekort
    DROP CONSTRAINT meldekort_hendelse_korrigert_meldekort_id_fkey;
ALTER TABLE meldekort_aktivitet
    DROP CONSTRAINT meldekort_aktivitet_meldekort_id_dato_fkey;
ALTER TABLE meldekort_dag
    DROP CONSTRAINT meldekort_dag_meldekort_id_fkey;


-- Steg 2: Bytt datatypen for meldekort_id og korrigert_av_meldekort_id til TEXT
ALTER TABLE meldekort
    ALTER COLUMN meldekort_id SET DATA TYPE TEXT
        USING meldekort_id::TEXT;

ALTER TABLE meldekort
    ALTER COLUMN korrigert_meldekort_id SET DATA TYPE TEXT
        USING korrigert_meldekort_id::TEXT;

ALTER TABLE meldekort
    ALTER COLUMN korrigert_av_meldekort_id SET DATA TYPE TEXT
        USING korrigert_av_meldekort_id::TEXT;

ALTER TABLE meldekort_dag
    ALTER COLUMN meldekort_id SET DATA TYPE TEXT
        USING meldekort_id::TEXT;

ALTER TABLE meldekort_aktivitet
    ALTER COLUMN meldekort_id SET DATA TYPE TEXT
        USING meldekort_id::TEXT;

-- Steg 3: Legg til den nye fremmednøkkelen med den oppdaterte datatypen
ALTER  TABLE meldekort
    ADD CONSTRAINT meldekort_korrigert_meldekort_id_fkey
        FOREIGN KEY (korrigert_meldekort_id) REFERENCES meldekort(meldekort_id);

ALTER TABLE meldekort
    ADD CONSTRAINT meldekort_korrigert_av_meldekort_id_fkey
        FOREIGN KEY (korrigert_av_meldekort_id) REFERENCES meldekort(meldekort_id);

ALTER TABLE meldekort_dag
    ADD CONSTRAINT meldekort_dag_meldekort_id_fkey
        FOREIGN KEY (meldekort_id) REFERENCES meldekort(meldekort_id);

ALTER TABLE meldekort_aktivitet
    ADD CONSTRAINT meldekort_aktivitet_meldekort_id_dato_fkey
        FOREIGN KEY (meldekort_id, dato) REFERENCES meldekort_dag(meldekort_id, dato);


-- Legg til indeks på korrigert_av_meldekort_id
DROP INDEX meldekort_korrigert_av_meldekort_id_idx;
CREATE INDEX meldekort_korrigert_av_meldekort_id_idx ON meldekort (korrigert_av_meldekort_id) WHERE korrigert_av_meldekort_id IS NULL;
