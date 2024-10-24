# § 4-28 Utestengning

## Regeltre

```mermaid
graph RL
  A["Utestengt"] -->|"Oppslag"| B["Prøvingsdato"]
  C["Oppfyller krav til ikke utestengt"] -->|"IngenAv"| A["Utestengt"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-utestengning
Egenskap: § 4-28 Utestengning

  Scenariomal: Søker oppfyller kravet om å ikke være utestengt
    Gitt at søker har søkt om dagpenger
    Og saksbehandler vurderer at søker er "<utestengt>"
    Så skal kravet om utestengning være "<oppfylt>"
  Eksempler:
     | utestengt | oppfylt   |
     | Ja        | Nei       |
     | Nei       | Ja        |
``` 