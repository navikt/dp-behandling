ALTER TABLE meldekort
    ADD COLUMN meldedato DATE;

UPDATE meldekort
SET meldedato = innsendt_tidspunkt::date;

ALTER TABLE meldekort
ALTER COLUMN meldedato SET NOT NULL;