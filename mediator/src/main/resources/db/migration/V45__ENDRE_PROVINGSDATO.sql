-- Bytter opplysning med navn Prøvingsdato|Virkningsdato til opplysning med navn Prøvingsdato|Prøvingsdato
-- Siste exist query sjekker om det finnes en opplysning med samme opplysningstype_id som den nye opplysningen, det finnes behandlinger som allerede har _både_ ny og gammel versjon av opplysningen.

--prod
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
