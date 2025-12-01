-- Fjern gamle indekser først
DROP INDEX idx_opplysning_fjernet_true;
DROP INDEX idx_opplysning_fjernet;

-- Partial index for både true og false. Oppslag bruker true, og sletting bruker false
CREATE INDEX IF NOT EXISTS idx_opplysning_fjernet_false ON opplysning (opplysninger_id) WHERE fjernet = FALSE;
CREATE INDEX IF NOT EXISTS idx_opplysning_fjernet_true ON opplysning (opplysninger_id) WHERE fjernet = TRUE;

-- Hent ut hvordan opplysningen ble utledet
CREATE INDEX IF NOT EXISTS opplysning_utledning_opplysning_id_idx ON opplysning_utledning (opplysning_id);

-- Hent ut hvilek opplysninger som ble brukt i utledningen
CREATE INDEX IF NOT EXISTS opplysning_utledet_av_opplysning_id_idx ON opplysning_utledet_av (opplysning_id);

-- Lag en indeks for kilde_id, siden mange opplysninger kan ha samme kilde
CREATE INDEX IF NOT EXISTS idx_opplysning_kilde_id ON opplysning (kilde_id);
