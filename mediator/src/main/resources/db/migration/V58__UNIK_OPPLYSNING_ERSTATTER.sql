ALTER TABLE opplysning_erstatter
    ADD CONSTRAINT opplysning_erstatter_pkey PRIMARY KEY (opplysning_id, erstatter_id);

-- Indeks for å kunne finne alle opplysninger som peker til samme erstatter
CREATE INDEX idx_opplysning_erstatter_erstatter_id ON opplysning_erstatter (erstatter_id);