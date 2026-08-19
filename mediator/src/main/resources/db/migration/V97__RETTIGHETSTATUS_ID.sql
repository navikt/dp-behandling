-- `opprettet` settes til NOW(), som Postgres fryser til starten av transaksjonen. Rader satt inn i samme
-- transaksjon (f.eks. batch-innsetting av hele rettighetshistorikken) får dermed identisk `opprettet`, og
-- `ORDER BY opprettet` gir da ingen garanti for rekkefølge ved rehydrering. Legger til en strengt økende
-- id-kolonne som gir en stabil, entydig sorteringsnøkkel uavhengig av fysisk lagringsrekkefølge.
ALTER TABLE rettighetstatus
    ADD COLUMN id BIGINT GENERATED ALWAYS AS IDENTITY;
