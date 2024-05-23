# § 4-6 Utdanning - første og andre avsnitt

## Regeltre

```mermaid
graph RL
  A["Deltar i arbeidsmarkedstiltak"] -->|"Oppslag"| B["Søknadstidspunkt"]
  C["Deltar i opplæring for innvandrere"] -->|"Oppslag"| B["Søknadstidspunkt"]
  D["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"] -->|"Oppslag"| B["Søknadstidspunkt"]
  E["Deltar i høyere yrkesfaglig utdanning"] -->|"Oppslag"| B["Søknadstidspunkt"]
  F["Deltar i høyere utdanning"] -->|"Oppslag"| B["Søknadstidspunkt"]
  G["Deltar på kurs mv"] -->|"Oppslag"| B["Søknadstidspunkt"]
  H["Godkjent unntak for utdanning eller opplæring?"] -->|"Alle"| A["Deltar i arbeidsmarkedstiltak"]
  H["Godkjent unntak for utdanning eller opplæring?"] -->|"Alle"| C["Deltar i opplæring for innvandrere"]
  H["Godkjent unntak for utdanning eller opplæring?"] -->|"Alle"| D["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"]
  H["Godkjent unntak for utdanning eller opplæring?"] -->|"Alle"| E["Deltar i høyere yrkesfaglig utdanning"]
  H["Godkjent unntak for utdanning eller opplæring?"] -->|"Alle"| F["Deltar i høyere utdanning"]
  H["Godkjent unntak for utdanning eller opplæring?"] -->|"Alle"| G["Deltar på kurs mv"]
  I["Har svart ja på spørsmål om utdanning eller opplæring"] -->|"ErSann"| J["Tar utdanning eller opplæring?"]
  K["Har svart nei på spørsmål om utdanning eller opplæring"] -->|"ErIkkeSann"| J["Tar utdanning eller opplæring?"]
  L["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| I["Har svart ja på spørsmål om utdanning eller opplæring"]
  L["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| H["Godkjent unntak for utdanning eller opplæring?"]
  M["Krav til utdanning eller opplæring"] -->|"EnAv"| L["Oppfyller kravet på unntak for utdanning eller opplæring"]
  M["Krav til utdanning eller opplæring"] -->|"EnAv"| K["Har svart nei på spørsmål om utdanning eller opplæring"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-utdanning
Egenskap: § 4-6 Utdanning - første og andre avsnitt

  Scenariomal: Søker oppfyller kravet til utdanning
    Gitt at personen søker på kravet om dagpenger
    Og at søkeren svarer "<utdanning>" på spørsmålet om utdanning
    Og søkeren har fått "<godkjent>" unntak til kravet om utdanning
    Så skal utfallet om utdanning være "<utfall>"

    Eksempler:
      | utdanning | godkjent | utfall |
      | Nei       | Nei      | Ja     |
      | Ja        | Nei      | Nei    |
      | Ja        | Ja       | Ja     |
``` 