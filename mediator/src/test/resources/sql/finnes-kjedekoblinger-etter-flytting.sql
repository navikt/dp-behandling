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
    SELECT bo.opplysninger_id, k.rot_id
    FROM kjede k
    JOIN behandling_opplysninger bo USING (behandling_id)
)
SELECT EXISTS (
    SELECT 1
    FROM eierskap fra
    JOIN opplysning o ON o.opplysninger_id = fra.opplysninger_id AND o.fjernet IS FALSE
    CROSS JOIN LATERAL (
        SELECT o.erstatter_id AS maal_id
        UNION
        SELECT o.identitet_id
        UNION
        SELECT utledet_av FROM opplysning_utledet_av WHERE opplysning_id = o.id
    ) r
    JOIN opplysning maal ON maal.id = r.maal_id
    WHERE NOT EXISTS (
        SELECT 1 FROM eierskap til
        WHERE til.rot_id = fra.rot_id AND til.opplysninger_id = maal.opplysninger_id
    )
) AS finnes_kobling_utenfor_kjeden;
