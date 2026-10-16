-- Sporer hvilke opplysninger som utløste at et kontrollpunkt (gjen)åpnet en avklaring.
-- Ingen FK mot opplysning(id): opplysninger et kontrollpunkt slår opp er ikke alltid
-- persistert av samme transaksjon (f.eks. transiente/beregnede opplysninger med
-- skalLagres=false, eller opplysninger som hører til en annen del av en behandlingskjede).
-- Dette er en sporings-/revisjonskobling, ikke en streng relasjon.
CREATE TABLE IF NOT EXISTS avklaring_endring_opplysning
(
    endring_id    uuid NOT NULL REFERENCES avklaring_endring (endring_id),
    opplysning_id uuid NOT NULL,
    PRIMARY KEY (endring_id, opplysning_id)
);
