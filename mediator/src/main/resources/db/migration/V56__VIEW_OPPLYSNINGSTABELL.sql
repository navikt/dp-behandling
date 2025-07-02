DROP VIEW opplysningstabell;

CREATE OR REPLACE VIEW opplysningstabell AS
SELECT opplysninger_opplysning.opplysninger_id,
       opplysning.id,
       opplysning.status,
       opplysningstype.datatype,
       opplysningstype.behov_id                                                                                  AS type_behov_id,
       opplysningstype.uuid                                                                                      AS type_uuid,
       opplysningstype.navn                                                                                      AS type_navn,
       opplysning.gyldig_fom,
       opplysning.gyldig_tom,
       opplysning_verdi.verdi_heltall,
       opplysning_verdi.verdi_desimaltall,
       opplysning_verdi.verdi_dato,
       opplysning_verdi.verdi_boolsk,
       opplysning_verdi.verdi_string,
       opplysning_utledning.regel                                                                                AS utledet_av,
       kilde.id                                                                                                  AS kilde_id,
       opplysning.opprettet,
       (SELECT ARRAY_AGG(oua.utledet_av) FROM opplysning_utledet_av oua WHERE oua.opplysning_id = opplysning.id) AS utledet_av_id,
       opplysning_verdi.verdi_jsonb,
       opplysning_erstatter.erstatter_id                                                                         AS erstatter_id,
       opplysningstype.formål                                                                                    AS type_formål
FROM opplysning
         LEFT JOIN
     opplysninger_opplysning ON opplysning.id = opplysninger_opplysning.opplysning_id
         LEFT JOIN
     opplysningstype ON opplysning.opplysningstype_id = opplysningstype.opplysningstype_id
         LEFT JOIN
     opplysning_verdi ON opplysning.id = opplysning_verdi.opplysning_id
         LEFT JOIN
     opplysning_utledning ON opplysning.id = opplysning_utledning.opplysning_id
         LEFT JOIN
     kilde ON kilde_id = kilde.id
         LEFT JOIN
     opplysning_erstatter ON opplysning.id = opplysning_erstatter.opplysning_id
WHERE opplysning.fjernet IS FALSE
ORDER BY opplysning.id;

-- Fjern den gamle tabellen som erstatter_id peker på
DROP TABLE opplysning_erstattet_av;