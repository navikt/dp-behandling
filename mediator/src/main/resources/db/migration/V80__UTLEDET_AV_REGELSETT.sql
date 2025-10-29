ALTER TABLE opplysning_utledning ADD COLUMN IF NOT EXISTS regelsett TEXT NULL;


-- Oppdater viewet til å ta med regelsett som har utledet opplysningen
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
       utledning.regelsett      AS regelsett_navn,
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
    SELECT regel, versjon, regelsett
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
