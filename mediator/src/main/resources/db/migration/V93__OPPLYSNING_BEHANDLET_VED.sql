-- Legger til kolonne for å spore hvilken prøvingsdato en opplysning ble behandlet ved.
-- Brukes av regelkjøringens cleanup for å fjerne opplysninger fra deaktiverte regelsett
-- selv når fraOgMed < prøvingsdato (f.eks. ønsketDato > søknadsdato).
ALTER TABLE opplysning ADD COLUMN behandlet_ved DATE DEFAULT NULL;

-- Oppdaterer viewet til å inkludere den nye kolonnen
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
       opplysningstype.formål   AS type_formål,
       opplysning.behandlet_ved
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
