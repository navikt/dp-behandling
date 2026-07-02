# § 4-10 Sanksjonsperiode ved selvforskyldt arbeidsløshet

## Regeltre

```mermaid
graph RL
  A["Beregnet antall dager med sanksjon"] -->|"Multiplikasjon"| B["Antall uker med sanksjon"]
  A["Beregnet antall dager med sanksjon"] -->|"Multiplikasjon"| C["Antall dager som skal regnes med i hver uke"]
  D["Antall dager med sanksjon"] -->|"HvisSannMedResultat"| E["Er ilagt sanskjonsperiode ved selvforskyldt arbeidsløshet"]
  D["Antall dager med sanksjon"] -->|"HvisSannMedResultat"| A["Beregnet antall dager med sanksjon"]
  D["Antall dager med sanksjon"] -->|"HvisSannMedResultat"| F["Ingen dager med sanksjon"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-sanksjon
Egenskap: § 4-10 Sanksjonsperiode ved selvforskyldt arbeidsløshet

  Scenario: Ingen sanksjon gir ingen sanksjonsdager
    Gitt at søker har søkt om dagpenger for vurdering av sanksjon
    Og saksbehandler ilegger ikke sanksjon
    Så skal antall sanksjonsdager være "0"

  Scenario: Sanksjon med standard antall uker gir 90 sanksjonsdager
    Gitt at søker har søkt om dagpenger for vurdering av sanksjon
    Og saksbehandler ilegger sanksjon
    Så skal antall sanksjonsdager være "90"

  Scenariomal: Antall sanksjonsdager beregnes ut fra antall sanksjonsuker
    Gitt at søker har søkt om dagpenger for vurdering av sanksjon
    Og saksbehandler ilegger sanksjon i "<uker>" uker
    Så skal antall sanksjonsdager være "<dager>"
  Eksempler:
    | uker | dager |
    | 18   | 90    |
    | 6    | 30    |
    | 1    | 5     |
``` 