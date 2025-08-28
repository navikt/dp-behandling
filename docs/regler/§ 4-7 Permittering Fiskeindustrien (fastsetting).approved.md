# § 4-7 Permittering Fiskeindustrien (fastsetting)

## Regeltre

```mermaid
graph RL
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-permitteringFiskeindustrien-fastsetting
Egenskap: § 4-7 Permittering Fiskeindustrien (fastsetting)

  Scenariomal: Søker oppfyller kravet til permittering fra fiskeindustrien under fastsetting
    Gitt at søker skal innvilges "<utfall>" med permittering fra fiskeindustrien
    Så skal søker få <periode> uker med permittering fra fiskeindustrien

  Eksempler:
   | utfall | periode |
   | Nei    | 0       |
   | Ja     | 52      |
``` 