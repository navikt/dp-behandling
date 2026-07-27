CREATE INDEX melding_lest_dato_brin
    ON melding
        USING brin (lest_dato);

CREATE INDEX melding_behandlet_tidspunkt_brin
    ON melding
        USING brin (behandlet_tidspunkt);