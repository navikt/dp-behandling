-- Legg til stabilt UUID-anker på person-tabellen
ALTER TABLE person ADD COLUMN person_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE person ADD CONSTRAINT person_person_id_unique UNIQUE (person_id);

-- Kobling mellom ident og person: en person kan ha flere identer
CREATE TABLE person_ident
(
    ident        TEXT        NOT NULL PRIMARY KEY,
    person_id    UUID        NOT NULL REFERENCES person (person_id),
    er_gjeldende BOOLEAN     NOT NULL DEFAULT TRUE,
    opprettet    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_person_ident_person_id ON person_ident (person_id);

-- Backfill fra eksisterende person-rader
INSERT INTO person_ident (ident, person_id, er_gjeldende)
SELECT ident, person_id, TRUE
FROM person;
