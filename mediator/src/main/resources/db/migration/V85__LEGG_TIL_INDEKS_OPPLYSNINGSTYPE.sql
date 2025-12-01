-- Lag en indeks for opplysningstype
CREATE INDEX IF NOT EXISTS idx_opplysning_opplysningstype_id ON opplysning (opplysningstype_id);
