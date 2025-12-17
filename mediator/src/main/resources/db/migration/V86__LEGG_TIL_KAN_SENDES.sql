ALTER TABLE meldekort
    ADD COLUMN kan_sendes_fra DATE;

update meldekort m
set kan_sendes_fra = to_date(md.kan_sendes_fra, 'YYYY-MM-DD')
from (
         select distinct on (ident, data ->> 'id') ident, data ->> 'id' as id, data ->> 'kanSendesFra' as kan_sendes_fra
         from melding
         where melding_type = 'MELDEKORT_INNSENDT'
         order by ident, data ->> 'id', lest_dato desc
     ) md
where md.ident = m.ident
  and md.id = m.meldekort_id;

ALTER TABLE meldekort
    ALTER COLUMN kan_sendes_fra SET NOT NULL;