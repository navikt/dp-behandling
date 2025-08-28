# § 4-15 Antall stønadsuker, stønadsperiode

## Regeltre

```mermaid
graph RL
  A["Terskel for 12 måneder"] -->|"Multiplikasjon"| B["Grunnbeløp"]
  A["Terskel for 12 måneder"] -->|"Multiplikasjon"| C["Terskelfaktor for 12 måneder"]
  D["Terskel for 36 måneder"] -->|"Multiplikasjon"| B["Grunnbeløp"]
  D["Terskel for 36 måneder"] -->|"Multiplikasjon"| E["Terskelfaktor for 36 måneder"]
  F["Snittinntekt siste 36 måneder"] -->|"Divisjon"| G["Arbeidsinntekt siste 36 måneder"]
  F["Snittinntekt siste 36 måneder"] -->|"Divisjon"| H["Antall år i 36 måneder"]
  I["Over terskel for 12 måneder"] -->|"StørreEnnEllerLik"| J["Arbeidsinntekt siste 12 måneder"]
  I["Over terskel for 12 måneder"] -->|"StørreEnnEllerLik"| A["Terskel for 12 måneder"]
  K["Over terskel for 36 måneder"] -->|"StørreEnnEllerLik"| F["Snittinntekt siste 36 måneder"]
  K["Over terskel for 36 måneder"] -->|"StørreEnnEllerLik"| D["Terskel for 36 måneder"]
  L["Stønadsuker ved siste 12 måneder"] -->|"HvisSannMedResultat"| I["Over terskel for 12 måneder"]
  L["Stønadsuker ved siste 12 måneder"] -->|"HvisSannMedResultat"| M["Lang dagpengeperiode"]
  L["Stønadsuker ved siste 12 måneder"] -->|"HvisSannMedResultat"| N["Kort dagpengeperiode"]
  O["Stønadsuker ved siste 36 måneder"] -->|"HvisSannMedResultat"| K["Over terskel for 36 måneder"]
  O["Stønadsuker ved siste 36 måneder"] -->|"HvisSannMedResultat"| M["Lang dagpengeperiode"]
  O["Stønadsuker ved siste 36 måneder"] -->|"HvisSannMedResultat"| N["Kort dagpengeperiode"]
  P["Antall stønadsuker"] -->|"HøyesteAv"| L["Stønadsuker ved siste 12 måneder"]
  P["Antall stønadsuker"] -->|"HøyesteAv"| O["Stønadsuker ved siste 36 måneder"]
  Q["Antall stønadsuker (stønadsperiode)"] -->|"HvisSannMedResultat"| R["Oppfyller kravet til minsteinntekt"]
  Q["Antall stønadsuker (stønadsperiode)"] -->|"HvisSannMedResultat"| P["Antall stønadsuker"]
  Q["Antall stønadsuker (stønadsperiode)"] -->|"HvisSannMedResultat"| S["Stønadsuker når kravet til minste arbeidsinntekt ikke er oppfylt"]
  T["Antall stønadsdager"] -->|"Multiplikasjon"| P["Antall stønadsuker"]
  T["Antall stønadsdager"] -->|"Multiplikasjon"| U["Antall dager som skal regnes med i hver uke"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-dapengeperiode
Egenskap: § 4-15 Antall stønadsuker, stønadsperiode

  Scenariomal: Søker har rett til dagpenger
    Gitt at søker har har rett til dagpenger fra <virkningstidspunkt>
    Og at søker har "<inntekt siste 12>" siste 12 måneder
    Og at søker har "<inntekt siste 36>" siste 36 måneder
    Så skal søker ha <antall uker> uker med dagpenger
    Eksempler:
      | virkningstidspunkt | inntekt siste 12 | inntekt siste 36 | antall uker |
      | 01.08.2024         | 300000           | 0                | 104         |
      | 01.08.2024         | 200000           | 0                | 52          |
      | 01.08.2024         | 0                | 300000           | 0           |
      | 01.08.2024         | 0                | 200000           | 0           |
      | 01.08.2024         | 0                | 748000           | 104         |
      | 01.08.2024         | 300000           | 200000           | 104         |
      | 01.08.2024         | 248056           | 0                | 104         |
      | 01.08.2024         | 248055           | 0                | 52          |
      | 01.08.2024         | 248055           | 0                | 52          |
      | 01.08.2024         | 248056           | 0                | 104         |
``` 