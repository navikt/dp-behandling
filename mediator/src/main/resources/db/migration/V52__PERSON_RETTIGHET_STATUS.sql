CREATE TABLE rettighetstatus
(
    ident         TEXT REFERENCES person (ident)             NOT NULL,
    gjelder_fra   TIMESTAMP WITH TIME ZONE                   NOT NULL,
    virkningsdato DATE                                       NOT NULL,
    har_rettighet BOOLEAN                                    NOT NULL,
    behandling_id UUID REFERENCES behandling (behandling_id) NOT NULL,
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE (behandling_id)
);