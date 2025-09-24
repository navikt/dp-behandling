CREATE INDEX idx_utboks_behandling_id
    ON utboks ((melding ->> 'behandlingId'));

CREATE INDEX idx_utboks_ident
    ON utboks ((melding ->> 'ident'));
