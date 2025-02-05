-- Bytter opplysning med navn Prøvingsdato|Virkningsdato til opplysning med navn Prøvingsdato|Prøvingsdato
-- Siste exist query sjekker om det finnes en opplysning med samme opplysningstype_id som den nye opplysningen, det finnes behandlinger som allerede har _både_ ny og gammel versjon av opplysningen.

--prod
-- +------------------+-------------+------------+--------+------+---------------------------------+------+------------------------------------+
-- |opplysningstype_id|behov_id     |navn        |datatype|parent|opprettet                        |formål|uuid                                |
-- +------------------+-------------+------------+--------+------+---------------------------------+------+------------------------------------+
-- |19216082          |Virkningsdato|Prøvingsdato|Dato    |null  |2024-10-21 08:47:35.475918 +00:00|Regel |e8cb6606-1e89-4733-b13d-b912ec4abf56|
-- |39494007          |Prøvingsdato |Prøvingsdato|Dato    |null  |2025-01-16 12:39:22.695917 +00:00|Regel |0194881f-91d1-7df2-ba1d-4533f37fcc76|
-- +------------------+-------------+------------+--------+------+---------------------------------+------+------------------------------------+


UPDATE opplysning SET opplysningstype_id = 39494007 WHERE opplysningstype_id = 19216082 AND id IN (
    SELECT id FROM opplysning
                       JOIN opplysninger_opplysning oo ON opplysning.id = oo.opplysning_id
                       JOIN behandling_opplysninger bo on oo.opplysninger_id = bo.opplysninger_id
                       JOIN behandling b ON bo.behandling_id = b.behandling_id
                       JOIN person_behandling pb ON b.behandling_id = pb.behandling_id
    WHERE b.tilstand NOT IN ('Avbrutt', 'Ferdig') AND opplysning.opplysningstype_id = 19216082
      AND NOT EXISTS (
        SELECT 1
        FROM opplysninger_opplysning oo2
                 JOIN opplysning o ON o.id = oo2.opplysning_id
                 JOIN opplysningstype o2 ON o2.opplysningstype_id = o.opplysningstype_id
        WHERE oo2.opplysning_id = opplysning.id AND o2.opplysningstype_id = 39494007
    ));

--dev
-- +------------------+-------------+------------+--------+------+---------------------------------+------+------------------------------------+
-- |opplysningstype_id|behov_id     |navn        |datatype|parent|opprettet                        |formål|uuid                                |
-- +------------------+-------------+------------+--------+------+---------------------------------+------+------------------------------------+
-- |636731            |Virkningsdato|Prøvingsdato|Dato    |null  |2024-10-21 10:45:06.628481 +00:00|Regel |b81a0b12-ec32-4135-b8e0-a7c60f84c759|
-- |6886412           |Prøvingsdato |Prøvingsdato|Dato    |null  |2025-01-16 12:07:21.151743 +00:00|Regel |0194881f-91d1-7df2-ba1d-4533f37fcc76|
-- +------------------+-------------+------------+--------+------+---------------------------------+------+------------------------------------+

UPDATE opplysning SET opplysningstype_id = 6886412 WHERE opplysningstype_id = 636731 AND id IN (
    SELECT id FROM opplysning
                       JOIN opplysninger_opplysning oo ON opplysning.id = oo.opplysning_id
                       JOIN behandling_opplysninger bo on oo.opplysninger_id = bo.opplysninger_id
                       JOIN behandling b ON bo.behandling_id = b.behandling_id
                       JOIN person_behandling pb ON b.behandling_id = pb.behandling_id
    WHERE b.tilstand NOT IN ('Avbrutt', 'Ferdig') AND opplysning.opplysningstype_id = 636731
      AND NOT EXISTS (
        SELECT 1
        FROM opplysninger_opplysning oo2
                 JOIN opplysning o ON o.id = oo2.opplysning_id
                 JOIN opplysningstype o2 ON o2.opplysningstype_id = o.opplysningstype_id
        WHERE oo2.opplysning_id = opplysning.id AND o2.opplysningstype_id = 6886412
    ));
