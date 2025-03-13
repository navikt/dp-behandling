ALTER TABLE behandler_hendelse DROP COLUMN fagsak_id;

ALTER TABLE behandler_hendelse ADD COLUMN forretningsprosess TEXT NOT NULL DEFAULT 'Søknadsprosess';