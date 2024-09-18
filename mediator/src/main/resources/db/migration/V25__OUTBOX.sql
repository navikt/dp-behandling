CREATE TABLE outbox
(
    id        SERIAL    PRIMARY KEY,
    key       TEXT      NULL,
    message   JSON      NOT NULL,
    status    TEXT      NOT NULL,
    opprettet TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);