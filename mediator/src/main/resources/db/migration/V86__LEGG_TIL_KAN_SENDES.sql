ALTER TABLE meldekort
    ADD COLUMN IF NOT EXISTS kan_sendes_fra DATE NULL;

UPDATE meldekort m
SET kan_sendes_fra = date_subtract(m.tom, '1 days');

UPDATE meldekort m
SET kan_sendes_fra = TO_DATE(md.kan_sendes_fra, 'YYYY-MM-DD')
FROM (SELECT DISTINCT ON (ident, data ->> 'id') ident, data ->> 'id' AS id, data ->> 'kanSendesFra' AS kan_sendes_fra
      FROM melding
      WHERE melding_type = 'MELDEKORT_INNSENDT'
      ORDER BY ident, data ->> 'id', lest_dato DESC) md
WHERE md.ident = m.ident
  AND md.id = m.meldekort_id;

ALTER TABLE meldekort
    ALTER COLUMN kan_sendes_fra SET NOT NULL;
