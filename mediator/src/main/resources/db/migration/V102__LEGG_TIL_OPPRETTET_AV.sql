ALTER TABLE behandler_hendelse
    ADD COLUMN opprettet_av_type TEXT NULL,
    ADD COLUMN opprettet_av_ident TEXT NULL,
    ADD CONSTRAINT behandler_hendelse_opprettet_av_konsistent CHECK (
        (opprettet_av_type IS NULL AND opprettet_av_ident IS NULL)
        OR (
            opprettet_av_type IS NOT NULL
            AND opprettet_av_ident IS NOT NULL
            AND opprettet_av_type IN ('Saksbehandler', 'System')
            AND NULLIF(BTRIM(opprettet_av_ident), '') IS NOT NULL
        )
    );
