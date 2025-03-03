CREATE TABLE meldekort_hendelse
(
    meldekort_id         BIGINT PRIMARY KEY NOT NULL,
    orginal_meldekort_id TEXT REFERENCES meldekort_hendelse (meldekort_id),
    meldingsreferanse_id UUID               NOT NULL REFERENCES melding (id),
    ident                TEXT               NOT NULL REFERENCES person (ident),
    fom                  DATE               NOT NULL,
    tom                  DATE               NOT NULL,
    kilde_rolle          TEXT               NOT NULL,
    kilde_ident          TEXT               NOT NULL,
    opprettet            TIMESTAMP          NOT NULL

);

CREATE TABLE meldekort_dag
(
    id           BIGINT PRIMARY KEY NOT NULL,
    meldekort_id BIGINT REFERENCES meldekort_hendelse (meldekort_id),
    dato         DATE               NOT NULL,
    UNIQUE (meldekort_id, dato)
);

CREATE TABLE meldekort_aktivitet
(
    dag_id BIGINT REFERENCES meldekort_dag (id),
    type   TEXT NOT NULL,
    timer  INTERVAL
);