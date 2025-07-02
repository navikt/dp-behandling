CREATE TABLE opplysning_erstatter
(
    opplysning_id uuid NOT NULL REFERENCES opplysning (id),
    erstatter_id  uuid NOT NULL REFERENCES opplysning (id),
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

INSERT INTO opplysning_erstatter (opplysning_id, erstatter_id, opprettet)
SELECT oea.erstattet_av, oea.opplysning_id, oea.opprettet
FROM opplysning_erstattet_av oea;
