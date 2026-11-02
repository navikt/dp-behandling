/* Supplerende typeoppslag som kjøres separat:
SELECT opplysningstype_id, navn
FROM opplysningstype
WHERE uuid = '01990a09-0eab-7957-b88f-14484a50e194';
*/
WITH RECURSIVE forfedre AS (
    SELECT behandling_id, basert_på_behandling_id
    FROM behandling
    WHERE behandling_id = ANY(ARRAY(
        SELECT behandling_id FROM avklaring WHERE kode = 'BehandlingFlyttet'
    ))

    UNION

    SELECT b.behandling_id, b.basert_på_behandling_id
    FROM behandling b
    JOIN forfedre f ON b.behandling_id = f.basert_på_behandling_id
),
kjede AS (
    SELECT behandling_id, behandling_id AS rot_id
    FROM forfedre
    WHERE basert_på_behandling_id IS NULL

    UNION ALL

    SELECT b.behandling_id, k.rot_id
    FROM behandling b
    JOIN kjede k ON b.basert_på_behandling_id = k.behandling_id
),
eierskap AS MATERIALIZED (
    SELECT bo.behandling_id, bo.opplysninger_id, k.rot_id
    FROM kjede k
    JOIN behandling_opplysninger bo USING (behandling_id)
)
SELECT fra.rot_id AS behandlingskjede_id,
       fra.behandling_id,
       o.opplysninger_id,
       o.id AS opplysning_id,
       o.opplysningstype_id,
       o.status,
       r.koblingstype,
       maal.id AS maal_opplysning_id,
       maal.opplysninger_id AS maal_opplysninger_id,
       maal.fjernet AS maal_fjernet
FROM eierskap fra
JOIN opplysning o ON o.opplysninger_id = fra.opplysninger_id AND o.fjernet IS FALSE
CROSS JOIN LATERAL (
    VALUES ('erstatter', o.erstatter_id), ('identitet', o.identitet_id)
    UNION ALL
    SELECT 'utledet_av', utledet_av FROM opplysning_utledet_av WHERE opplysning_id = o.id
) r(koblingstype, maal_id)
JOIN opplysning maal ON maal.id = r.maal_id
WHERE NOT EXISTS (
    SELECT 1 FROM eierskap til
    WHERE til.rot_id = fra.rot_id AND til.opplysninger_id = maal.opplysninger_id
);