CREATE TABLE behandling_aktive_behov
(
    behandling_id uuid      NOT NULL,
    behov         TEXT      NOT NULL,
    opprettet     TIMESTAMP NOT NULL,
    status        TEXT      NOT NULL,
    PRIMARY KEY (behandling_id, behov),
    FOREIGN KEY (behandling_id) REFERENCES behandling (behandling_id)
)