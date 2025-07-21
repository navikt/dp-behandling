# § 4-6 Utdanning - første og andre avsnitt

## Regeltre

```mermaid
graph RL
  A["Brukeren er under utdanning eller opplæring"] -->|"Ekstern"| B["søknadId"]
  C["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| D["Deltar i arbeidsmarkedstiltak"]
  C["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| E["Deltar i opplæring for innvandrere"]
  C["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| F["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"]
  C["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| G["Deltar i høyere yrkesfaglig utdanning"]
  C["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| H["Deltar i høyere utdanning"]
  C["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| I["Deltar på kurs mv"]
  J["Har svart ja på spørsmål om utdanning eller opplæring"] -->|"ErSann"| A["Brukeren er under utdanning eller opplæring"]
  K["Har svart nei på spørsmål om utdanning eller opplæring"] -->|"ErUsann"| A["Brukeren er under utdanning eller opplæring"]
  L["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| J["Har svart ja på spørsmål om utdanning eller opplæring"]
  L["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| C["Godkjent unntak for utdanning eller opplæring?"]
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
    Og at unntaket arbeidsmarkedstiltak er "<arbeidsmarkedstiltak>"
    Og at unntaket opplæring for innvandrere er "<opplæring for innvandrere>"
    Og at unntaket grunnskoleopplæring er "<grunnskoleopplæring>"
    Og at unntaket høyere yrkesfaglig utdanning er "<høyere yrkesfaglig utdanning>"
    Og at unntaket høyere utdanning er "<høyere utdanning>"
    Og at unntaket deltar på kurs er "<deltar på kurs>"
    Så skal utfallet om utdanning være "<utfall>"

    Eksempler:
      | utdanning | arbeidsmarkedstiltak | opplæring for innvandrere | grunnskoleopplæring | høyere yrkesfaglig utdanning | høyere utdanning | deltar på kurs | utfall |
      | Nei       | Nei                  | Nei                       | Nei                 | Nei                          | Nei              | Nei            | Ja     |
      | Ja        | Nei                  | Nei                       | Nei                 | Nei                          | Nei              | Nei            | Nei    |
      | Ja        | Ja                   | Nei                       | Nei                 | Nei                          | Nei              | Nei            | Ja     |
      | Ja        | Nei                  | Ja                        | Nei                 | Nei                          | Nei              | Nei            | Ja     |
      | Ja        | Nei                  | Nei                       | Ja                  | Nei                          | Nei              | Nei            | Ja     |
      | Ja        | Nei                  | Nei                       | Nei                 | Ja                           | Nei              | Nei            | Ja     |
      | Ja        | Nei                  | Nei                       | Nei                 | Nei                          | Ja               | Nei            | Ja     |
      | Ja        | Nei                  | Nei                       | Nei                 | Nei                          | Nei              | Ja             | Ja     |
``` 