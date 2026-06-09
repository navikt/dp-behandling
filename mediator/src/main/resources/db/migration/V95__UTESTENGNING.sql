CREATE TABLE utestengning
(
    id                  BIGSERIAL PRIMARY KEY,
    ident               VARCHAR(11)  NOT NULL,
    behandling_id       UUID         NOT NULL,
    behandlingskjede_id UUID         NOT NULL,
    fra_og_med          DATE         NOT NULL,
    til_og_med          DATE         NOT NULL,
    opprettet           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_utestengning_behandling UNIQUE (ident, behandling_id)
);

CREATE INDEX idx_utestengning_ident ON utestengning (ident);
