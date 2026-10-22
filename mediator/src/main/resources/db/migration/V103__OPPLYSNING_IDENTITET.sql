-- En revisjon av en opplysning er en ny rad med en ny id, men samme identitet som raden den revideres fra.
-- NULL betyr at raden er sin egen identitet (identitet_id = id).
ALTER TABLE opplysning ADD COLUMN identitet_id UUID DEFAULT NULL;

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
       opplysning.behandlet_ved,
       opplysning.identitet_id
FROM opplysning
         INNER JOIN
     opplysningstype ON opplysning.opplysningstype_id = opplysningstype.opplysningstype_id
         LEFT JOIN
     kilde ON kilde_id = kilde.id

         LEFT JOIN LATERAL (
    SELECT regel, versjon
    FROM opplysning_utledning
    WHERE opplysning_id = opplysning.id
    LIMIT 1
    ) utledning ON TRUE

         LEFT JOIN LATERAL (
    SELECT ARRAY_AGG(utledet_av) AS utledet_av_id
    FROM opplysning_utledet_av
    WHERE opplysning_id = opplysning.id
    ) utledet_av_ids ON TRUE
WHERE opplysning.fjernet IS FALSE;
