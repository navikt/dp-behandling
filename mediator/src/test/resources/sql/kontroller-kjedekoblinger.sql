-- Bruk kontroller-kjedekoblinger.psql for automatisk kartlegging av hele databasen.
-- Sett etter_id til neste_etter_id fra forrige batch. Start med NULL.
-- Tellingene gjelder kun denne batchen, ikke hele databasen.
WITH RECURSIVE parametre AS NOT MATERIALIZED (
    SELECT NULL::uuid AS etter_id, 1000 AS batchstorrelse
),
batch AS MATERIALIZED (
    SELECT o.id, o.opplysninger_id, o.erstatter_id, o.identitet_id, o.fjernet
    FROM opplysning o
    CROSS JOIN parametre p
    WHERE p.etter_id IS NULL OR o.id > p.etter_id
    ORDER BY o.id
    LIMIT (SELECT batchstorrelse FROM parametre)
),
referanser AS MATERIALIZED (
    SELECT o.opplysninger_id, r.referansetype, r.maal_id
    FROM batch o
    CROSS JOIN LATERAL (
        VALUES ('erstatter', o.erstatter_id), ('identitet', o.identitet_id)
    ) r(referansetype, maal_id)
    WHERE o.fjernet IS FALSE AND r.maal_id IS NOT NULL

    UNION ALL

    SELECT o.opplysninger_id, 'utledet_av', ua.utledet_av
    FROM batch o
    JOIN opplysning_utledet_av ua ON ua.opplysning_id = o.id
    WHERE o.fjernet IS FALSE AND ua.utledet_av IS NOT NULL
),
maalopplysninger AS MATERIALIZED (
    SELECT id, opplysninger_id, fjernet
    FROM opplysning
    WHERE id = ANY(ARRAY(SELECT maal_id FROM referanser))
),
eiere AS MATERIALIZED (
    SELECT bo.opplysninger_id, bo.behandling_id
    FROM behandling_opplysninger bo
    WHERE bo.opplysninger_id = ANY(ARRAY(
        SELECT opplysninger_id FROM batch
        UNION
        SELECT opplysninger_id FROM maalopplysninger
    ))
),
forfedre AS (
    SELECT behandling_id, basert_på_behandling_id
    FROM behandling
    WHERE behandling_id = ANY(ARRAY(SELECT behandling_id FROM eiere))

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

    SELECT f.behandling_id, k.rot_id
    FROM forfedre f
    JOIN kjede k ON f.basert_på_behandling_id = k.behandling_id
),
eierskap AS MATERIALIZED (
    SELECT e.opplysninger_id, e.behandling_id, k.rot_id
    FROM eiere e
    LEFT JOIN kjede k USING (behandling_id)
),
avvik AS (
    SELECT r.referansetype,
           CASE
               WHEN fra.behandling_id IS NULL THEN 'mangler kildebehandling'
               WHEN fra.rot_id IS NULL THEN 'kildekjede uten rot'
               WHEN maal.id IS NULL THEN 'mangler maalopplysning'
               WHEN til.behandling_id IS NULL THEN 'mangler maalbehandling'
               WHEN til.rot_id IS NULL THEN 'maalkjede uten rot'
               ELSE 'ulik behandlingskjede'
           END AS avvikstype,
           maal.fjernet AS maal_fjernet,
           fra.behandling_id,
           fra.rot_id
    FROM referanser r
    LEFT JOIN eierskap fra ON fra.opplysninger_id = r.opplysninger_id
    LEFT JOIN maalopplysninger maal ON maal.id = r.maal_id
    LEFT JOIN eierskap til ON til.opplysninger_id = maal.opplysninger_id
    WHERE fra.rot_id IS NULL OR til.rot_id IS NULL OR fra.rot_id <> til.rot_id
),
oppsummering AS (
    SELECT referansetype,
           avvikstype,
           maal_fjernet,
           COUNT(*) AS antall_koblinger,
           COUNT(DISTINCT behandling_id) AS antall_behandlinger,
           COUNT(DISTINCT rot_id) AS antall_kjeder
    FROM avvik
    GROUP BY referansetype, avvikstype, maal_fjernet
),
batchstatus AS (
    SELECT COUNT(*) AS antall_leste,
           COUNT(*) FILTER (WHERE fjernet IS FALSE) AS antall_aktive,
           (SELECT id FROM batch ORDER BY id DESC LIMIT 1) AS neste_etter_id,
           COUNT(*) < (SELECT batchstorrelse FROM parametre) AS ferdig
    FROM batch
)
SELECT b.antall_leste,
       b.antall_aktive,
       b.neste_etter_id,
       b.ferdig,
       o.referansetype,
       o.avvikstype,
       o.maal_fjernet,
       COALESCE(o.antall_koblinger, 0) AS antall_koblinger,
       COALESCE(o.antall_behandlinger, 0) AS antall_behandlinger,
       COALESCE(o.antall_kjeder, 0) AS antall_kjeder
FROM batchstatus b
LEFT JOIN oppsummering o ON TRUE
ORDER BY o.referansetype, o.avvikstype, o.maal_fjernet;
