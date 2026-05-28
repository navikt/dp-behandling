CREATE TABLE oppdatering_feed
(
    id            BIGSERIAL PRIMARY KEY,
    opprettet     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    hendelse_id   UUID        NOT NULL,
    ident         TEXT,
    behandling_id UUID,
    type          TEXT        NOT NULL,
    payload       JSONB       NOT NULL,
    payload_hash  TEXT        NOT NULL,
    kilde         TEXT        NOT NULL DEFAULT 'dp-behandling',
    UNIQUE (hendelse_id, type, payload_hash)
);

CREATE INDEX oppdatering_feed_ident_id_idx ON oppdatering_feed (ident, id);
CREATE INDEX oppdatering_feed_behandling_id_id_idx ON oppdatering_feed (behandling_id, id);
