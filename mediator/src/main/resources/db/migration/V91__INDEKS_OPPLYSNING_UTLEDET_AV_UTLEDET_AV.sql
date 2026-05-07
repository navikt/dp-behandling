-- Gjenoppretter indeks som ble droppet i V63.
-- Sletting i VaktmesterPostgresRepo har et OR-predikat på (opplysning_id OR utledet_av),
-- og uten denne indeksen blir det seq scan av hele tabellen per slettebatch.
-- I prod er indeksen allerede opprettet manuelt med CREATE INDEX CONCURRENTLY,
-- så IF NOT EXISTS gjør at denne migreringen blir en no-op der.
CREATE INDEX IF NOT EXISTS idx_opplysning_utledet_av_utledet_av
    ON opplysning_utledet_av (utledet_av);
