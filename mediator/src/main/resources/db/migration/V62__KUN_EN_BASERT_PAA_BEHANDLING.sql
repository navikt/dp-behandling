-- 1. Legg til ny kolonne i behandling
ALTER TABLE behandling
    ADD COLUMN basert_på_behandling_id uuid;

-- 2. Oppdater behandling med verdier fra behandling_basertpå
UPDATE behandling b
SET basert_på_behandling_id = bb.basert_på_behandling_id
FROM behandling_basertpå bb
WHERE b.behandling_id = bb.behandling_id;

-- 3. Legg på FOREIGN KEY constraint
ALTER TABLE behandling
    ADD CONSTRAINT fk_behandling_basert_på
        FOREIGN KEY (basert_på_behandling_id)
            REFERENCES behandling (behandling_id);

-- 4. Slett behandling_basertpå hvis du ikke trenger den lenger
DROP TABLE behandling_basertpå;