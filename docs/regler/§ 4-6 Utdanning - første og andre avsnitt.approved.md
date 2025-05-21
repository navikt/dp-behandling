# § 4-6 Utdanning - første og andre avsnitt

## Regeltre

```mermaid
graph RL
  A["Brukeren er under utdanning eller opplæring"] -->|"Ekstern"| B["søknadId"]
  C["Deltar i arbeidsmarkedstiltak"] -->|"Oppslag"| D["Prøvingsdato"]
  E["Deltar i opplæring for innvandrere"] -->|"Oppslag"| D["Prøvingsdato"]
  F["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"] -->|"Oppslag"| D["Prøvingsdato"]
  G["Deltar i høyere yrkesfaglig utdanning"] -->|"Oppslag"| D["Prøvingsdato"]
  H["Deltar i høyere utdanning"] -->|"Oppslag"| D["Prøvingsdato"]
  I["Deltar på kurs mv"] -->|"Oppslag"| D["Prøvingsdato"]
  J["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| C["Deltar i arbeidsmarkedstiltak"]
  J["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| E["Deltar i opplæring for innvandrere"]
  J["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| F["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"]
  J["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| G["Deltar i høyere yrkesfaglig utdanning"]
  J["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| H["Deltar i høyere utdanning"]
  J["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| I["Deltar på kurs mv"]
  K["Har svart ja på spørsmål om utdanning eller opplæring"] -->|"ErSann"| A["Brukeren er under utdanning eller opplæring"]
  L["Har svart nei på spørsmål om utdanning eller opplæring"] -->|"ErUsann"| A["Brukeren er under utdanning eller opplæring"]
  M["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| K["Har svart ja på spørsmål om utdanning eller opplæring"]
  M["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| J["Godkjent unntak for utdanning eller opplæring?"]
  N["Krav til utdanning eller opplæring"] -->|"EnAv"| M["Oppfyller kravet på unntak for utdanning eller opplæring"]
  N["Krav til utdanning eller opplæring"] -->|"EnAv"| L["Har svart nei på spørsmål om utdanning eller opplæring"]
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