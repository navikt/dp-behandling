# § 4-7 Permittering

## Regeltre

```mermaid
graph RL
  A["Oppfyller kravet til permittering"] -->|"Alle"| B["Skal permittering vurderes"]
  A["Oppfyller kravet til permittering"] -->|"Alle"| C["Årsaken til permitteringen er godkjent"]
  A["Oppfyller kravet til permittering"] -->|"Alle"| D["Permitteringen er midlertidig driftsinnskrenkning eller driftsstans"]
  A["Oppfyller kravet til permittering"] -->|"Alle"| E["Innenfor fritaksperioden for tap av arbeidsinntekt"]
  F["Dato permitteringen løper fra"] -->|"FraOgMedForOpplysning"| A["Oppfyller kravet til permittering"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-permittering
Egenskap: § 4-7 Permittering

  Scenariomal: Søker oppfyller kravet til permittering
    Gitt at søker har "<er permittert>" om dagpenger under permittering
    Og saksbehandler vurderer at søker har "<godkjent årsak>" til permittering
    Og vurderer at søker har "<midlertidig>" permittering
    Så skal søker få "<utfall>" av permittering

  Eksempler:
    | er permittert | godkjent årsak | midlertidig | utfall |
    | Nei           | Nei            | Nei         | Nei    |
    | Nei           | Ja             | Nei         | Nei    |
    | Nei           | Nei            | Ja          | Nei    |
    | Ja            | Nei            | Nei         | Nei    |
    | Ja            | Ja             | Nei         | Nei    |
    | Ja            | Nei            | Ja          | Nei    |
    | Ja            | Ja             | Ja          | Ja     |
``` 