-- rettighetstatus er en fullstendig cache som skrives om i sin helhet hver gang en person lagres (se
-- PersonRepositoryPostgres.lagre). Datamodellen endres fra brytepunkter til reelle fraOgMed/tilOgMed-
-- perioder nøkkelet på regelverk, i en ny tabell rettighetstidslinje. Gammel tabell TRUNCATES *ikke* og
-- slettes heller ikke - den beholdes under navnet rettighetstatus_pre_v103 som arkiv/forensisk referanse,
-- siden den inneholder gjelder_fra som ikke har noen plass i den nye modellen. MeldekortBehandlingskø
-- krever overlapp mellom meldekortperioden og en rettighetstidslinje-periode for i det hele tatt å sende
-- meldekortet til behandling - uten backfill ville tabellen stått tom til evig tid for løpende saker,
-- siden selve mekanismen som skulle fylt den på nytt (meldekortbehandling) er gatet på at den allerede
-- har innhold.

DROP INDEX idx_rettighetstatus_ident_gjelder_fra;

ALTER TABLE rettighetstatus
    RENAME TO rettighetstatus_pre_v103;

CREATE TABLE rettighetstidslinje
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ident               TEXT        NOT NULL REFERENCES person (ident),
    regelverk           TEXT        NOT NULL,
    fra_og_med          DATE        NOT NULL,
    til_og_med          DATE        NOT NULL,
    har_rettighet       BOOLEAN     NOT NULL,
    behandling_id       UUID        NOT NULL REFERENCES behandling (behandling_id),
    behandlingskjede_id UUID        NOT NULL REFERENCES behandling (behandling_id),
    opplysning_id       UUID        NOT NULL,
    opprettet           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Alle rader i den gamle tabellen stammer fra før regelverk-oppdelingen og representerte utelukkende
-- Dagpenger (Utestengning og Ferietillegg hadde egne/separate mekanismer, se V95 og V104).
--
-- opplysning_id har ingen historisk kilde (brukes til å spore opphavet til en periode som videreføres
-- uendret mellom behandlinger i samme kjede, se Rettighetstidslinje.oppdater). Eksisterende rader får en
-- fast nil-UUID som plassholder - ekte opplysningId-er er alltid UUIDv7 (tidsbaserte) og vil derfor aldri
-- kunne kollidere med denne. Første reelle lagring av personen etter denne migreringen erstatter dem uansett.
--
-- til_og_med utledes fra neste brytepunkts virkningsdato (dagen før). Siste brytepunkt per ident var en
-- åpen periode i den gamle modellen og blir derfor stående uten sluttdato (infinity).
WITH neste_brytepunkt AS (
    SELECT id,
           ident,
           virkningsdato,
           har_rettighet,
           behandling_id,
           behandlingskjede_id,
           LEAD(virkningsdato) OVER (PARTITION BY ident ORDER BY virkningsdato, id) AS neste_virkningsdato
    FROM rettighetstatus_pre_v103
)
INSERT INTO rettighetstidslinje (ident, regelverk, fra_og_med, til_og_med, har_rettighet, behandling_id,
                                  behandlingskjede_id, opplysning_id)
SELECT ident,
       'Dagpenger',
       virkningsdato,
       COALESCE(neste_virkningsdato - 1, 'infinity'::date),
       har_rettighet,
       behandling_id,
       behandlingskjede_id,
       '00000000-0000-0000-0000-000000000000'::uuid
FROM neste_brytepunkt
ORDER BY ident, virkningsdato, id;

CREATE INDEX idx_rettighetstidslinje_ident_fra_og_med ON rettighetstidslinje (ident, fra_og_med DESC);
