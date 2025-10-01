CREATE TABLE IF NOT EXISTS utbetaling_status (
    behandling_id UUID REFERENCES behandling(behandling_id),
    meldekort_id VARCHAR REFERENCES meldekort(meldekort_id),
    status TEXT NOT NULL,
    endret TIMESTAMP WITH TIME ZONE NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (behandling_id, meldekort_id)
);


