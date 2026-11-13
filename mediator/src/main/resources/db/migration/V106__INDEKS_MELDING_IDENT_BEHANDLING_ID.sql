-- flyway:executeInTransaction=false
CREATE INDEX CONCURRENTLY IF NOT EXISTS melding_ident_idx
    ON melding (ident);

CREATE INDEX CONCURRENTLY IF NOT EXISTS melding_behandling_id_idx
    ON melding ((data ->> 'behandlingId'))
    WHERE (data ->> 'behandlingId') IS NOT NULL;
