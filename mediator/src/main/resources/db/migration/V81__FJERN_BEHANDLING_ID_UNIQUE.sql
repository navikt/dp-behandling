-- 1. Legg til ny kolonne
ALTER TABLE rettighetstatus
    ADD COLUMN behandlingskjede_id uuid;

-- 2. Fyll ut behandlingskjede_id ved å finne roten i kjeden
WITH RECURSIVE chain AS (
    -- start på alle behandlinger brukt i rettighetstatus
    SELECT r.ident,
           r.behandling_id AS original_behandling_id,
           b.behandling_id,
           b.basert_på_behandling_id
    FROM rettighetstatus r
             JOIN behandling b ON b.behandling_id = r.behandling_id

    UNION ALL

    -- gå oppover kjeden så lenge det finnes en basert_på
    SELECT c.ident,
           c.original_behandling_id,
           b2.behandling_id,
           b2.basert_på_behandling_id
    FROM chain c
             JOIN behandling b2 ON b2.behandling_id = c.basert_på_behandling_id)

-- 3. Velg "roten" (den som har basert_på IS NULL) og bruk den til å oppdatere rettighetstatus
UPDATE rettighetstatus r
SET behandlingskjede_id = root.behandling_id
FROM (SELECT DISTINCT ON (original_behandling_id) original_behandling_id,
                                                  behandling_id
      FROM chain
      WHERE basert_på_behandling_id IS NULL
      ORDER BY original_behandling_id) root
WHERE r.behandling_id = root.original_behandling_id;

-- 4. Legg til NOT NULL-constraint (etter at kolonnen er fylt)
ALTER TABLE rettighetstatus
    ALTER COLUMN behandlingskjede_id SET NOT NULL,
    DROP CONSTRAINT rettighetstatus_behandling_id_key;

-- (Valgfritt) Legg til FK til behandlingstabellen
ALTER TABLE rettighetstatus
    ADD CONSTRAINT rettighetstatus_behandlingskjede_fkey
        FOREIGN KEY (behandlingskjede_id)
            REFERENCES behandling (behandling_id);
