# § 4-7 Permittering Fiskeindustrien

## Regeltre

```mermaid
graph RL
  A["Årsaken til permitteringen fra fiskeindustrien er godkjent"] -->|"Oppslag"| B["Prøvingsdato"]
  C["Permitteringen fra fiskeindustrien er midlertidig driftsinnskrenkning eller driftsstans"] -->|"Oppslag"| B["Prøvingsdato"]
  D["Krav til prosentvis tap av arbeidstid ved permittering fra fiskeindustrien"] -->|"Oppslag"| B["Prøvingsdato"]
  E["Oppfyller kravet til permittering i fiskeindustrien"] -->|"Alle"| F["Permittert fra fiskeindustrien"]
  E["Oppfyller kravet til permittering i fiskeindustrien"] -->|"Alle"| A["Årsaken til permitteringen fra fiskeindustrien er godkjent"]
  E["Oppfyller kravet til permittering i fiskeindustrien"] -->|"Alle"| C["Permitteringen fra fiskeindustrien er midlertidig driftsinnskrenkning eller driftsstans"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-permittering-fiskeindustrien
Egenskap: § 4-7 Permittering Fiskeindustrien

  Scenariomal: Søker oppfyller kravet til permittering fra fiskeindustrien
    Gitt at søker har "<er permittert>" om dagpenger under permittering fra fiskeindustrien
    Og saksbehandler vurderer at søker har "<godkjent årsak>" til permittering fra fiskeindustrien
    Og vurderer at søker har "<midlertidig>" permittering fra fiskeindustrien
    Så skal søker få "<utfall>" av permittering fra fiskeindustrien

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