CREATE OR REPLACE VIEW opplysningstabell AS
WITH opplysningstyper AS MATERIALIZED (
    SELECT opplysningstype_id, datatype, behov_id, uuid, navn, formål
    FROM opplysningstype
)
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
       CASE WHEN utledning.opplysning_id IS NOT NULL THEN (
           SELECT ARRAY_AGG(utledet_av)
           FROM opplysning_utledet_av
           WHERE opplysning_id = opplysning.id
       ) END                   AS utledet_av_id,
       opplysning.verdi_jsonb,
       opplysning.erstatter_id  AS erstatter_id,
       opplysningstype.formål   AS type_formål,
       opplysning.behandlet_ved,
       opplysning.identitet_id
FROM opplysning
         INNER JOIN opplysningstyper opplysningstype
                    ON opplysning.opplysningstype_id = opplysningstype.opplysningstype_id
         LEFT JOIN kilde ON opplysning.kilde_id = kilde.id
         LEFT JOIN opplysning_utledning utledning ON utledning.opplysning_id = opplysning.id
WHERE opplysning.fjernet IS FALSE;
