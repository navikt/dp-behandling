CREATE TABLE outbox
(
    id        UUID PRIMARY KEY,
    key       TEXT      NULL,
    json      JSON      NOT NULL,
    status    TEXT      NOT NULL,
    opprettet TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);