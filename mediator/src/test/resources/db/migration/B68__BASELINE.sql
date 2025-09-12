CREATE SEQUENCE meldekort_hendelse_løpenummer_seq;

CREATE TABLE IF NOT EXISTS melding
(
    id                  BIGSERIAL
        PRIMARY KEY,
    ident               TEXT                                   NOT NULL,
    melding_id          uuid                                   NOT NULL
        UNIQUE,
    melding_type        TEXT                                   NOT NULL,
    data                jsonb                                  NOT NULL,
    lest_dato           TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    behandlet_tidspunkt TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS opplysningstype
(
    opplysningstype_id BIGSERIAL
        PRIMARY KEY,
    behov_id           TEXT NOT NULL,
    navn               TEXT NOT NULL,
    datatype           TEXT NOT NULL,
    parent             BIGINT
        REFERENCES opplysningstype,
    opprettet          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    formål             TEXT NOT NULL,
    uuid               uuid NOT NULL,
    CONSTRAINT unik_id
        UNIQUE (uuid, datatype)
);

CREATE TABLE IF NOT EXISTS kilde
(
    id         uuid                     NOT NULL
        CONSTRAINT opplysning_kilde_pkey
            PRIMARY KEY,
    type       TEXT                     NOT NULL,
    opprettet  TIMESTAMP WITH TIME ZONE NOT NULL,
    registrert TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS opplysning
(
    id                 uuid                     NOT NULL
        PRIMARY KEY,
    status             TEXT                     NOT NULL,
    opplysningstype_id BIGINT                   NOT NULL
        REFERENCES opplysningstype,
    gyldig_fom         DATE,
    gyldig_tom         DATE,
    opprettet          TIMESTAMP WITH TIME ZONE NOT NULL,
    kilde_id           uuid
        CONSTRAINT kilde_id_fkey
            REFERENCES kilde,
    fjernet            BOOLEAN DEFAULT FALSE    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_opplysning_opplysningstype_id
    ON opplysning (opplysningstype_id);

CREATE INDEX IF NOT EXISTS idx_opplysning_fjernet_true
    ON opplysning (opplysningstype_id ASC, id ASC, opprettet DESC)
    WHERE (fjernet = TRUE);

CREATE INDEX IF NOT EXISTS idx_opplysning_fjernet
    ON opplysning (fjernet)
    WHERE (fjernet = FALSE);

CREATE TABLE IF NOT EXISTS opplysning_verdi
(
    opplysning_id     uuid NOT NULL
        PRIMARY KEY
        REFERENCES opplysning,
    datatype          TEXT,
    verdi_heltall     INTEGER,
    verdi_desimaltall NUMERIC,
    verdi_dato        DATE,
    verdi_boolsk      BOOLEAN,
    verdi_string      TEXT,
    opprettet         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    verdi_jsonb       jsonb
);

CREATE INDEX IF NOT EXISTS idx_opplysning_verdi_opplysning_id
    ON opplysning_verdi (opplysning_id);

CREATE INDEX IF NOT EXISTS idx_opplysning_fagsak_id
    ON opplysning_verdi (verdi_heltall)
    WHERE (verdi_heltall IS NOT NULL);

CREATE TABLE IF NOT EXISTS kilde_system
(
    kilde_id   uuid NOT NULL
        CONSTRAINT opplysning_kilde_system_pkey
            PRIMARY KEY
        CONSTRAINT opplysning_kilde_system_kilde_id_fkey
            REFERENCES kilde,
    melding_id uuid
        CONSTRAINT opplysning_kilde_system_melding_id_fkey
            REFERENCES melding (melding_id)
);

CREATE TABLE IF NOT EXISTS kilde_saksbehandler
(
    kilde_id                uuid NOT NULL
        CONSTRAINT opplysning_kilde_saksbehandler_pkey
            PRIMARY KEY
        CONSTRAINT opplysning_kilde_saksbehandler_kilde_id_fkey
            REFERENCES kilde,
    ident                   TEXT NOT NULL,
    melding_id              uuid NOT NULL,
    begrunnelse             TEXT,
    begrunnelse_sist_endret TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS opplysninger
(
    opplysninger_id uuid NOT NULL
        PRIMARY KEY,
    opprettet       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS opplysninger_opplysning
(
    opplysninger_id uuid
        REFERENCES opplysninger,
    opplysning_id   uuid
        REFERENCES opplysning,
    CONSTRAINT unik_kobling
        UNIQUE (opplysninger_id, opplysning_id)
);

CREATE INDEX IF NOT EXISTS idx_opplysninger_opplysning_id
    ON opplysninger_opplysning (opplysning_id);

CREATE INDEX IF NOT EXISTS idx_opplysninger_opplysninger_id
    ON opplysninger_opplysning (opplysninger_id);

CREATE TABLE IF NOT EXISTS behandling
(
    behandling_id           uuid                                   NOT NULL
        PRIMARY KEY,
    tilstand                TEXT                                   NOT NULL,
    opprettet               TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    sist_endret_tilstand    TIMESTAMP                DEFAULT NOW() NOT NULL,
    basert_på_behandling_id uuid
        CONSTRAINT fk_behandling_basert_på
            REFERENCES behandling
);

CREATE TABLE IF NOT EXISTS behandler_hendelse
(
    melding_id         uuid                                NOT NULL
        PRIMARY KEY,
    ident              TEXT                                NOT NULL,
    ekstern_id         TEXT                                NOT NULL,
    hendelse_type      TEXT                                NOT NULL,
    skjedde            TIMESTAMP WITH TIME ZONE            NOT NULL,
    forretningsprosess TEXT DEFAULT 'Søknadsprosess'::TEXT NOT NULL,
    ekstern_id_type    TEXT DEFAULT 'SøknadId'::TEXT       NOT NULL
);

CREATE TABLE IF NOT EXISTS behandler_hendelse_behandling
(
    behandling_id uuid NOT NULL
        REFERENCES behandling,
    melding_id    uuid NOT NULL
        REFERENCES behandler_hendelse,
    CONSTRAINT behandler_hendelse_behandling_unik_kobling
        UNIQUE (behandling_id, melding_id)
);

CREATE TABLE IF NOT EXISTS behandling_opplysninger
(
    behandling_id   uuid NOT NULL
        REFERENCES behandling,
    opplysninger_id uuid NOT NULL
        REFERENCES opplysninger,
    CONSTRAINT behandling_opplysninger_unik_kobling
        UNIQUE (behandling_id, opplysninger_id)
);

CREATE INDEX IF NOT EXISTS idx_behandling_opplysninger
    ON behandling_opplysninger (behandling_id);

CREATE TABLE IF NOT EXISTS person
(
    id        BIGSERIAL
        PRIMARY KEY,
    ident     TEXT NOT NULL
        UNIQUE,
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS person_behandling
(
    ident         TEXT NOT NULL
        REFERENCES person (ident),
    behandling_id uuid NOT NULL
        REFERENCES behandling,
    CONSTRAINT person_behandling_unik_kobling
        UNIQUE (ident, behandling_id)
);

CREATE TABLE IF NOT EXISTS opplysning_utledning
(
    opplysning_id uuid NOT NULL
        PRIMARY KEY
        REFERENCES opplysning,
    regel         TEXT,
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS opplysning_utledet_av
(
    opplysning_id uuid
        REFERENCES opplysning_utledning,
    utledet_av    uuid
        REFERENCES opplysning,
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (opplysning_id, utledet_av)
);

CREATE TABLE IF NOT EXISTS avklaring
(
    id            uuid    NOT NULL
        PRIMARY KEY,
    behandling_id uuid    NOT NULL
        REFERENCES behandling,
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    kode          TEXT    NOT NULL,
    tittel        TEXT    NOT NULL,
    beskrivelse   TEXT    NOT NULL,
    kan_kvitteres BOOLEAN NOT NULL,
    kan_avbrytes  BOOLEAN                  DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS avklaring_behandling_id_idx
    ON avklaring (behandling_id);

CREATE TABLE IF NOT EXISTS avklaring_endring
(
    endring_id   uuid      NOT NULL
        PRIMARY KEY,
    avklaring_id uuid      NOT NULL
        REFERENCES avklaring,
    endret       TIMESTAMP NOT NULL,
    type         TEXT      NOT NULL,
    opprettet    TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    kilde_id     uuid
        REFERENCES kilde,
    begrunnelse  TEXT
);

CREATE INDEX IF NOT EXISTS avklaring_endring_avklaring_id_idx
    ON avklaring_endring (avklaring_id);

CREATE TABLE IF NOT EXISTS behandling_tilstand
(
    id            BIGSERIAL
        PRIMARY KEY,
    behandling_id uuid                     NOT NULL
        REFERENCES behandling,
    tilstand      TEXT                     NOT NULL,
    endret        TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (behandling_id, tilstand, endret)
);

CREATE TABLE IF NOT EXISTS behandling_arbeidssteg
(
    behandling_id uuid NOT NULL
        REFERENCES behandling,
    oppgave       TEXT NOT NULL,
    tilstand      TEXT NOT NULL,
    utført_av     TEXT,
    utført        TIMESTAMP WITH TIME ZONE,
    CONSTRAINT behandling_arbeidssteg_unik_oppgave
        UNIQUE (behandling_id, oppgave)
);

CREATE TABLE IF NOT EXISTS behandling_aktive_behov
(
    behandling_id uuid      NOT NULL
        REFERENCES behandling,
    behov         TEXT      NOT NULL,
    opprettet     TIMESTAMP NOT NULL,
    status        TEXT      NOT NULL,
    PRIMARY KEY (behandling_id, behov)
);

CREATE TABLE IF NOT EXISTS meldekort
(
    id                        uuid                                                                                      NOT NULL
        CONSTRAINT meldekort_hendelse_pkey
            PRIMARY KEY,
    løpenummer                BIGINT                   DEFAULT NEXTVAL('"meldekort_hendelse_løpenummer_seq"'::regclass) NOT NULL,
    meldekort_id              TEXT                                                                                      NOT NULL
        CONSTRAINT meldekort_hendelse_meldekort_id_key
            UNIQUE,
    korrigert_meldekort_id    TEXT
        REFERENCES meldekort (meldekort_id),
    meldingsreferanse_id      uuid                                                                                      NOT NULL
        CONSTRAINT meldekort_hendelse_meldingsreferanse_id_fkey
            REFERENCES melding (melding_id),
    ident                     TEXT                                                                                      NOT NULL
        CONSTRAINT meldekort_hendelse_ident_fkey
            REFERENCES person (ident),
    innsendt_tidspunkt        TIMESTAMP                                                                                 NOT NULL,
    fom                       DATE                                                                                      NOT NULL,
    tom                       DATE                                                                                      NOT NULL,
    kilde_rolle               TEXT                                                                                      NOT NULL,
    kilde_ident               TEXT                                                                                      NOT NULL,
    opprettet                 TIMESTAMP WITH TIME ZONE DEFAULT NOW()                                                    NOT NULL,
    behandling_startet        TIMESTAMP,
    behandling_ferdig         TIMESTAMP,
    korrigert_av_meldekort_id TEXT
        REFERENCES meldekort (meldekort_id)
);

ALTER SEQUENCE meldekort_hendelse_løpenummer_seq OWNED BY meldekort.løpenummer;

CREATE INDEX IF NOT EXISTS meldekort_hendelse_ident_idx
    ON meldekort (ident);

CREATE INDEX IF NOT EXISTS meldekort_korrigert_av_meldekort_id_idx
    ON meldekort (korrigert_av_meldekort_id)
    WHERE (korrigert_av_meldekort_id IS NULL);

CREATE TABLE IF NOT EXISTS meldekort_dag
(
    meldekort_id TEXT    NOT NULL
        REFERENCES meldekort (meldekort_id),
    meldt        BOOLEAN NOT NULL,
    dato         DATE    NOT NULL,
    PRIMARY KEY (meldekort_id, dato)
);

CREATE TABLE IF NOT EXISTS meldekort_aktivitet
(
    meldekort_id TEXT NOT NULL,
    dato         DATE NOT NULL,
    type         TEXT NOT NULL,
    timer        INTERVAL,
    FOREIGN KEY (meldekort_id, dato) REFERENCES meldekort_dag
);

CREATE TABLE IF NOT EXISTS rettighetstatus
(
    ident         TEXT                     NOT NULL
        REFERENCES person (ident),
    gjelder_fra   TIMESTAMP WITH TIME ZONE NOT NULL,
    virkningsdato DATE                     NOT NULL,
    har_rettighet BOOLEAN                  NOT NULL,
    behandling_id uuid                     NOT NULL
        UNIQUE
        REFERENCES behandling,
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS opplysning_erstatter
(
    opplysning_id uuid NOT NULL
        REFERENCES opplysning,
    erstatter_id  uuid NOT NULL
        REFERENCES opplysning,
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (opplysning_id, erstatter_id)
);

CREATE INDEX IF NOT EXISTS idx_opplysning_erstatter_erstatter_id
    ON opplysning_erstatter (erstatter_id);

CREATE TABLE IF NOT EXISTS utboks
(
    id        SERIAL
        PRIMARY KEY,
    melding   jsonb NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE VIEW opplysningstabell
            (opplysninger_id, id, status, datatype, type_behov_id, type_uuid, type_navn, gyldig_fom, gyldig_tom,
             verdi_heltall, verdi_desimaltall, verdi_dato, verdi_boolsk, verdi_string, utledet_av, kilde_id, opprettet,
             utledet_av_id, verdi_jsonb, erstatter_id, type_formål)
AS
SELECT opplysninger_opplysning.opplysninger_id,
       opplysning.id,
       opplysning.status,
       opplysningstype.datatype,
       opplysningstype.behov_id AS type_behov_id,
       opplysningstype.uuid     AS type_uuid,
       opplysningstype.navn     AS type_navn,
       opplysning.gyldig_fom,
       opplysning.gyldig_tom,
       opplysning_verdi.verdi_heltall,
       opplysning_verdi.verdi_desimaltall,
       opplysning_verdi.verdi_dato,
       opplysning_verdi.verdi_boolsk,
       opplysning_verdi.verdi_string,
       utledning.regel          AS utledet_av,
       kilde.id                 AS kilde_id,
       opplysning.opprettet,
       utledet_av_ids.utledet_av_id,
       opplysning_verdi.verdi_jsonb,
       opplysning_erstatter.erstatter_id,
       opplysningstype."formål" AS "type_formål"
FROM opplysninger_opplysning
         JOIN opplysning ON opplysninger_opplysning.opplysning_id = opplysning.id
         JOIN opplysningstype ON opplysning.opplysningstype_id = opplysningstype.opplysningstype_id
         JOIN opplysning_verdi ON opplysning.id = opplysning_verdi.opplysning_id
         LEFT JOIN kilde ON opplysning.kilde_id = kilde.id
         LEFT JOIN opplysning_erstatter ON opplysning.id = opplysning_erstatter.opplysning_id
         LEFT JOIN LATERAL ( SELECT opplysning_utledning.regel
                             FROM opplysning_utledning
                             WHERE opplysning_utledning.opplysning_id = opplysning.id
                             LIMIT 1) utledning ON TRUE
         LEFT JOIN LATERAL ( SELECT ARRAY_AGG(opplysning_utledet_av.utledet_av) AS utledet_av_id
                             FROM opplysning_utledet_av
                             WHERE opplysning_utledet_av.opplysning_id = opplysning.id) utledet_av_ids ON TRUE
WHERE opplysning.fjernet IS FALSE;

CREATE VIEW meldingslogg (id, meldingsreferanse_id, type, behandling_id, ident, opprettet, melding) AS
SELECT id,
       melding ->> '@id'::TEXT          AS meldingsreferanse_id,
       melding ->> '@event_name'::TEXT  AS type,
       melding ->> 'behandlingId'::TEXT AS behandling_id,
       melding ->> 'ident'::TEXT        AS ident,
       opprettet,
       melding
FROM utboks;

