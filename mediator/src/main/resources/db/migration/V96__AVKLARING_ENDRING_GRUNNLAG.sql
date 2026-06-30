-- Lagrer hvilke opplysninger som trigget en UnderBehandling-endring (avklaringsgrunnlaget).
-- Brukes til å avgjøre om en avbrutt avklaring skal gjenåpnes: gjenåpne kun om
-- minst én av grunnlagsopplysningene er erstattet. Eksisterende rader uten grunnlag
-- behandles som "grunnlag ukjent — aldri gjenåpne" (tom mengde).
CREATE TABLE IF NOT EXISTS avklaring_endring_grunnlag
(
    endring_id    uuid NOT NULL REFERENCES avklaring_endring (endring_id),
    opplysning_id uuid NOT NULL,
    PRIMARY KEY (endring_id, opplysning_id)
);

CREATE INDEX IF NOT EXISTS avklaring_endring_grunnlag_endring_id_idx ON avklaring_endring_grunnlag (endring_id);
