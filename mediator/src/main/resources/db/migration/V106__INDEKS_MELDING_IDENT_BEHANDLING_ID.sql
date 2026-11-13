CREATE INDEX IF NOT EXISTS melding_ident_idx
    ON melding (ident);

CREATE INDEX IF NOT EXISTS melding_behandling_id_idx
    ON melding ((data ->> 'behandlingId'))
    WHERE (data ->> 'behandlingId') IS NOT NULL;
