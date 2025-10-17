ALTER TABLE opplysning
    ADD COLUMN opplysninger_id   uuid NULL DEFAULT NULL,
    ADD COLUMN datatype          TEXT,
    ADD COLUMN verdi_heltall     INTEGER,
    ADD COLUMN verdi_desimaltall NUMERIC,
    ADD COLUMN verdi_dato        DATE,
    ADD COLUMN verdi_boolsk      BOOLEAN,
    ADD COLUMN verdi_string      TEXT,
    ADD COLUMN verdi_jsonb       jsonb,
    ADD COLUMN erstatter_id      uuid NULL REFERENCES opplysning
;

-- Fyll inn med opplysninger_id for eksisterende rader fra opplysninger_opplysning
UPDATE opplysning o
SET opplysninger_id = oo.opplysninger_id
FROM opplysninger_opplysning oo
WHERE o.id = oo.opplysning_id;

-- Fyll inn de andre kolonnene fra opplysning_verdi
UPDATE opplysning o
SET datatype          = ov.datatype,
    verdi_heltall     = ov.verdi_heltall,
    verdi_desimaltall = ov.verdi_desimaltall,
    verdi_dato        = ov.verdi_dato,
    verdi_boolsk      = ov.verdi_boolsk,
    verdi_string      = ov.verdi_string
FROM opplysning_verdi ov
WHERE o.id = ov.opplysning_id;

-- Inline erstattet
UPDATE opplysning o
SET erstatter_id = oe.erstatter_id
FROM opplysning_erstatter oe
WHERE o.id = oe.opplysning_id;

-- Endre opplysning.opplysninger_id til NOT NULL etter at data er migrert
ALTER TABLE opplysning
    ALTER COLUMN opplysninger_id SET NOT NULL;

-- Lag en foreign key constraint for å sikre referanseintegritet
ALTER TABLE opplysning
    ADD CONSTRAINT fk_opplysning_opplysninger
        FOREIGN KEY (opplysninger_id)
            REFERENCES opplysninger (opplysninger_id);

-- Lag indeks for bedre ytelse på foreign key
CREATE INDEX idx_opplysning_opplysninger_id ON opplysning (opplysninger_id);

-- Oppdater viewet til å ikke joine
DROP VIEW opplysningstabell;
CREATE OR REPLACE VIEW opplysningstabell AS
SELECT opplysning.opplysninger_id,
       opplysning.id,
       opplysning.status,
       opplysningstype.datatype,
       opplysningstype.behov_id AS type_behov_id,
       opplysningstype.uuid     AS type_uuid,
       opplysningstype.navn     AS type_navn,
       opplysning.gyldig_fom,
       opplysning.gyldig_tom,
       opplysning.verdi_heltall,
       opplysning.verdi_desimaltall,
       opplysning.verdi_dato,
       opplysning.verdi_boolsk,
       opplysning.verdi_string,
       utledning.regel          AS utledet_av,
       utledning.versjon        AS utledet_versjon,
       kilde.id                 AS kilde_id,
       opplysning.opprettet,
       utledet_av_ids.utledet_av_id,
       opplysning.verdi_jsonb,
       opplysning.erstatter_id  AS erstatter_id,
       opplysningstype.formål   AS type_formål
FROM opplysning
         INNER JOIN
     opplysningstype ON opplysning.opplysningstype_id = opplysningstype.opplysningstype_id
         LEFT JOIN
     kilde ON kilde_id = kilde.id

         -- Lateral subquery: henter regel fra én rad
         LEFT JOIN LATERAL (
    SELECT regel, versjon
    FROM opplysning_utledning
    WHERE opplysning_id = opplysning.id
    LIMIT 1
    ) utledning ON TRUE

    -- Lateral subquery: henter liste med utledet_av-id-er
         LEFT JOIN LATERAL (
    SELECT ARRAY_AGG(utledet_av) AS utledet_av_id
    FROM opplysning_utledet_av
    WHERE opplysning_id = opplysning.id
    ) utledet_av_ids ON TRUE
WHERE opplysning.fjernet IS FALSE;

-- Slett tabeller som er inlinet
-- DROP TABLE opplysninger_opplysning;
-- DROP TABLE opplysning_verdi;
-- DROP TABLE opplysning_erstatter;
