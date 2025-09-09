CREATE TABLE utboks
(
    id        SERIAL PRIMARY KEY,
    melding   jsonb NOT NULL,
    opprettet timestamptz DEFAULT NOW()
);

CREATE OR REPLACE VIEW meldingslogg AS
SELECT id,
       melding ->> '@id'          AS meldingsreferanse_id,
       melding ->> '@event_name'  AS type,
       melding ->> 'behandlingId' AS behandling_id,
       melding ->> 'ident'        AS ident,
       opprettet,
       melding
FROM utboks;

