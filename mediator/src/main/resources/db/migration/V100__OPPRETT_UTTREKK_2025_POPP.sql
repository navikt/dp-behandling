-- Uttrekk for POPP 2025. Inneholder sum utbetaling, totalt barnetillegg og uavkortet grunnlag for 12 og 36 måneder.
CREATE TABLE IF NOT EXISTS uttrekk_2025_popp AS
WITH r2 AS (SELECT DISTINCT r.ident
            FROM rettighetstatus r
            WHERE r.har_rettighet = TRUE
              AND r.virkningsdato BETWEEN '2025-01-01' AND '2025-12-31')
SELECT p.ident,

       SUM(SPLIT_PART(o.verdi_string, ' ', 2)::NUMERIC)
       FILTER (
           WHERE ot.uuid = '01957069-d7d5-7f7c-b359-c00686fbf1f7'
           AND o.gyldig_fom BETWEEN '2025-01-01' AND '2025-12-31'
           )                                                                      AS sum_utbetaling_2025,

       MIN(SPLIT_PART(o.verdi_string, ' ', 2)::NUMERIC)
       FILTER (WHERE ot.uuid = '0194881f-9428-74d5-b160-f63a4c61a244')
           *
       COUNT(*)
       FILTER (
           WHERE ot.uuid = '01957069-d7d5-7f7c-b359-c00686fbf1f7'
           AND o.gyldig_fom BETWEEN '2025-01-01' AND '2025-12-31'
           AND SPLIT_PART(o.verdi_string, ' ', 2)::NUMERIC > 0
           )                                                                      AS totalt_barnetillegg,

       ROUND(MAX(SPLIT_PART(o.verdi_string, ' ', 2)::NUMERIC)
             FILTER (WHERE ot.uuid = '0194881f-9410-7481-b263-4606fdd10cb0'), 0)  AS uavkortet_grunnlag_12mnd,

       ROUND((MAX(SPLIT_PART(o.verdi_string, ' ', 2)::NUMERIC)
              FILTER (WHERE ot.uuid = '0194881f-9410-7481-b263-4606fdd10cb0')
           + MAX(SPLIT_PART(o.verdi_string, ' ', 2)::NUMERIC)
             FILTER (WHERE ot.uuid = '0194881f-9410-7481-b263-4606fdd10cb1')
           + MAX(SPLIT_PART(o.verdi_string, ' ', 2)::NUMERIC)
             FILTER (WHERE ot.uuid = '0194881f-9410-7481-b263-4606fdd10cb2')), 0) AS uavkortet_grunnlag_36mnd
FROM person p
         JOIN r2 ON r2.ident = p.ident
         JOIN person_behandling pb ON pb.ident = p.ident
         JOIN behandling_opplysninger bo ON bo.behandling_id = pb.behandling_id
         JOIN opplysning o ON o.opplysninger_id = bo.opplysninger_id
         JOIN opplysningstype ot ON ot.opplysningstype_id = o.opplysningstype_id
WHERE o.fjernet IS FALSE
  AND ot.uuid IN (
                  '01957069-d7d5-7f7c-b359-c00686fbf1f7', -- utbetaling (per dag)
                  '0194881f-9428-74d5-b160-f63a4c61a244', -- Barnetillegg
                  '0194881f-9410-7481-b263-4606fdd10cb0', -- Grunnlag periode 1
                  '0194881f-9410-7481-b263-4606fdd10cb1', -- Grunnlag periode 2
                  '0194881f-9410-7481-b263-4606fdd10cb2' -- Grunnlag periode 3
    )
GROUP BY p.ident
ORDER BY sum_utbetaling_2025 DESC;
