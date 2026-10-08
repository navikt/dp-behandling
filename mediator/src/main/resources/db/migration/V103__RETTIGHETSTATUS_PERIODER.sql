-- rettighetstatus er en fullstendig cache som skrives om i sin helhet hver gang en person lagres (se
-- PersonRepositoryPostgres.lagre). Datamodellen endres fra brytepunkter til reelle fraOgMed/tilOgMed-
-- perioder nøkkelet på regelverk. Eksisterende rader TRUNCATES *ikke* - MeldekortBehandlingskø krever
-- overlapp mellom meldekortperioden og en rettighetstatus-periode for i det hele tatt å sende meldekortet
-- til behandling. Uten backfill ville tabellen stått tom til evig tid for løpende saker, siden selve
-- mekanismen som skulle fylt den på nytt (meldekortbehandling) er gatet på at den allerede har innhold.
DROP INDEX idx_rettighetstatus_ident_gjelder_fra;

ALTER TABLE rettighetstatus
    RENAME COLUMN virkningsdato TO fra_og_med;

ALTER TABLE rettighetstatus
    ADD COLUMN til_og_med DATE,
    ADD COLUMN regelverk TEXT,
    ADD COLUMN opplysning_id UUID;

-- Alle eksisterende rader stammer fra før regelverk-oppdelingen og representerte utelukkende Dagpenger
-- (Utestengning og Ferietillegg hadde egne/separate mekanismer, se V95 og V104).
UPDATE rettighetstatus
SET regelverk = 'Dagpenger';

-- opplysning_id har ingen historisk kilde (brukes til å spore opphavet til en periode som videreføres
-- uendret mellom behandlinger i samme kjede, se Rettighetstidslinje.oppdater). Eksisterende rader får en
-- fast nil-UUID som plassholder - ekte opplysningId-er er alltid UUIDv7 (tidsbaserte) og vil derfor aldri
-- kunne kollidere med denne. Første reelle lagring av personen etter denne migreringen erstatter dem uansett.
UPDATE rettighetstatus
SET opplysning_id = '00000000-0000-0000-0000-000000000000'::uuid;

-- til_og_med utledes fra neste brytepunkts fra_og_med (dagen før). Siste brytepunkt per ident var en åpen
-- periode i den gamle modellen og blir derfor stående uten sluttdato (infinity).
WITH neste_brytepunkt AS (
    SELECT id,
           LEAD(fra_og_med) OVER (PARTITION BY ident ORDER BY fra_og_med, id) AS neste_fra_og_med
    FROM rettighetstatus
)
UPDATE rettighetstatus r
SET til_og_med = COALESCE(n.neste_fra_og_med - 1, 'infinity'::date)
FROM neste_brytepunkt n
WHERE r.id = n.id;

ALTER TABLE rettighetstatus
    ALTER COLUMN til_og_med SET NOT NULL,
    ALTER COLUMN regelverk SET NOT NULL,
    ALTER COLUMN opplysning_id SET NOT NULL;

ALTER TABLE rettighetstatus
    DROP COLUMN gjelder_fra;

CREATE INDEX idx_rettighetstatus_ident_fra_og_med ON rettighetstatus (ident, fra_og_med DESC);
