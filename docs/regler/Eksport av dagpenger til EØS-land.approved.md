# Eksport av dagpenger til EØS-land

## Regeltre

```mermaid
graph RL
  A["Bruker skal ha eksport av dagpenger til EØS-land"] -->|"ErSann"| B["Skal eksport vurderes"]
  C["Dato eksport skal gjelde fra"] -->|"FraOgMedForOpplysning"| B["Skal eksport vurderes"]
  D["Fristdato for å registrere seg i vertsland"] -->|"LeggTilDager"| C["Dato eksport skal gjelde fra"]
  D["Fristdato for å registrere seg i vertsland"] -->|"LeggTilDager"| E["Antall dagers frist for å registrere seg i vertsland"]
  F["Bruker er registrert i vertsland"] -->|"Utgangspunkt"| A["Bruker skal ha eksport av dagpenger til EØS-land"]
  G["Dato bruker registrerte seg i vertsland"] -->|"FraOgMedForOpplysning"| F["Bruker er registrert i vertsland"]
  H["Bruker er registrert innen fristen i vertsland"] -->|"FørEllerLik"| G["Dato bruker registrerte seg i vertsland"]
  H["Bruker er registrert innen fristen i vertsland"] -->|"FørEllerLik"| D["Fristdato for å registrere seg i vertsland"]
  I["Bruker er registrert etter fristen i vertsland"] -->|"IngenAv"| H["Bruker er registrert innen fristen i vertsland"]
  J["Dato for gjenopptak av dagpenger"] -->|"HvisSannMedResultat"| H["Bruker er registrert innen fristen i vertsland"]
  J["Dato for gjenopptak av dagpenger"] -->|"HvisSannMedResultat"| C["Dato eksport skal gjelde fra"]
  J["Dato for gjenopptak av dagpenger"] -->|"HvisSannMedResultat"| G["Dato bruker registrerte seg i vertsland"]
  K["Oppfyller vilkåret om eksport av dagpenger til EØS-land"] -->|"AlleMedGyldighetsperiodeFra"| A["Bruker skal ha eksport av dagpenger til EØS-land"]
  K["Oppfyller vilkåret om eksport av dagpenger til EØS-land"] -->|"AlleMedGyldighetsperiodeFra"| F["Bruker er registrert i vertsland"]
  K["Oppfyller vilkåret om eksport av dagpenger til EØS-land"] -->|"AlleMedGyldighetsperiodeFra"| J["Dato for gjenopptak av dagpenger"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-eksport
Egenskap: Eksport av dagpenger til EØS-land

  Scenario: Søker oppfyller vilkåret om eksport når registrering skjer innen fristen
    Gitt at eksport av dagpenger skal vurderes fra "01.02.2025"
    Og at personen registrerer seg i vertslandet "05.02.2025"
    Så skal vilkåret om eksport være oppfylt fra og med "01.02.2025"

  Scenario: Søker oppfyller vilkåret om eksport når registrering skjer etter fristen
    Gitt at eksport av dagpenger skal vurderes fra "01.02.2025"
    Og at personen registrerer seg i vertslandet "15.02.2025"
    Så skal vilkåret om eksport være oppfylt fra og med "15.02.2025"

  Scenario: Søker oppfyller ikke vilkåret om eksport uten registrering i vertslandet
    Gitt at eksport av dagpenger skal vurderes fra "01.02.2025"
    Så skal vilkåret om eksport ikke være oppfylt
``` 