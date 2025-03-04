CREATE TABLE meldekort_hendelse
(
    id                     UUID PRIMARY KEY NOT NULL,
    løpenummer             BIGSERIAL        NOT NULL,
    meldekort_id           BIGINT           NOT NULL UNIQUE,
    korrigert_meldekort_id BIGINT REFERENCES meldekort_hendelse (meldekort_id),
    meldingsreferanse_id   UUID             NOT NULL REFERENCES melding (melding_id),
    ident                  TEXT             NOT NULL REFERENCES person (ident),
    innsendt_tidspunkt     TIMESTAMP        NOT NULL,
    fom                    DATE             NOT NULL,
    tom                    DATE             NOT NULL,
    kilde_rolle            TEXT             NOT NULL,
    kilde_ident            TEXT             NOT NULL,
    opprettet              TIMESTAMP        NOT NULL,
);

CREATE INDEX meldekort_hendelse_ident_idx ON meldekort_hendelse (ident);

CREATE TABLE meldekort_dag
(

    meldekort_id BIGINT REFERENCES meldekort_hendelse (meldekort_id),
    meldt        BOOLEAN NOT NULL,
    dato         DATE    NOT NULL,
    PRIMARY KEY (meldekort_id, dato)
);

CREATE TABLE meldekort_aktivitet
(
    meldekort_id BIGINT NOT NULL,
    dato         DATE   NOT NULL,
    type         TEXT   NOT NULL, -- Arbeid, Fravær, Syk, Utdanning
    timer        INTERVAL,
    FOREIGN KEY (meldekort_id, dato) REFERENCES meldekort_dag (meldekort_id, dato)

);