# § 4-19 Verneplikt (fastsetting)

## Regeltre

```mermaid
graph RL
  A["Antall G som gis som grunnlag ved verneplikt"] -->|"Oppslag"| B["Prøvingsdato"]
  C["Grunnlag for gis ved verneplikt"] -->|"Multiplikasjon"| D["Grunnbeløp for grunnlag"]
  C["Grunnlag for gis ved verneplikt"] -->|"Multiplikasjon"| A["Antall G som gis som grunnlag ved verneplikt"]
  E["Periode som gis ved verneplikt"] -->|"Oppslag"| B["Prøvingsdato"]
  F["Fastsatt vanlig arbeidstid for verneplikt"] -->|"Oppslag"| B["Prøvingsdato"]
  G["Grunnlag for verneplikt hvis kravet er oppfylt"] -->|"HvisRegel"| H["Har utført minst tre måneders militærtjeneste eller obligatorisk sivilforsvarstjeneste"]
  G["Grunnlag for verneplikt hvis kravet er oppfylt"] -->|"HvisRegel"| C["Grunnlag for gis ved verneplikt"]
  I["Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget"] -->|"StørreEnn"| C["Grunnlag for gis ved verneplikt"]
  I["Grunnlaget for verneplikt er høyere enn dagpengegrunnlaget"] -->|"StørreEnn"| J["Grunnlag ved ordinære dagpenger"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-verneplikt-fastsetting
Egenskap: § 4-19 Verneplikt (fastsetting)

  Scenario: Gitt at søker oppfyller kravet verneplikt
    Gitt at søker har søkt om dagpenger under verneplikt 19.08.2024
    Så skal grunnlag være 372084
    Og dagpengerperioden være 26 uker
``` 