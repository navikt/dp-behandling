DROP VIEW opplysningstabell;

CREATE OR REPLACE VIEW opplysningstabell AS
SELECT opplysninger_opplysning.opplysninger_id,
       opplysning.id,
       opplysning.status,
       opplysningstype.datatype,
       opplysningstype.behov_id          AS type_behov_id,
       opplysningstype.uuid              AS type_uuid,
       opplysningstype.navn              AS type_navn,
       opplysning.gyldig_fom,
       opplysning.gyldig_tom,
       opplysning_verdi.verdi_heltall,
       opplysning_verdi.verdi_desimaltall,
       opplysning_verdi.verdi_dato,
       opplysning_verdi.verdi_boolsk,
       opplysning_verdi.verdi_string,
       utledning.regel        AS utledet_av,
       kilde.id                          AS kilde_id,
       opplysning.opprettet,
       utledet_av_ids.utledet_av_id,
       opplysning_verdi.verdi_jsonb,
       opplysning_erstatter.erstatter_id AS erstatter_id,
       opplysningstype.formål            AS type_formål
FROM opplysninger_opplysning
         INNER JOIN
     opplysning ON opplysninger_opplysning.opplysning_id = opplysning.id
         INNER JOIN
     opplysningstype ON opplysning.opplysningstype_id = opplysningstype.opplysningstype_id
         INNER JOIN
     opplysning_verdi ON opplysning.id = opplysning_verdi.opplysning_id
         LEFT JOIN
     kilde ON kilde_id = kilde.id
         LEFT JOIN
     opplysning_erstatter ON opplysning.id = opplysning_erstatter.opplysning_id

         -- Lateral subquery: henter regel fra én rad
         LEFT JOIN LATERAL (
    SELECT regel
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
