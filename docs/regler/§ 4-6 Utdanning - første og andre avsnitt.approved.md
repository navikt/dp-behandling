# § 4-6 Utdanning - første og andre avsnitt

## Regeltre

```mermaid
graph RL
  A["Brukeren er under utdanning eller opplæring"] -->|"Ekstern"| B["søknadId"]
  A["Brukeren er under utdanning eller opplæring"] -->|"Ekstern"| C["Søknadsdato"]
  D["Deltar i arbeidsmarkedstiltak"] -->|"Oppslag"| E["Prøvingsdato"]
  F["Deltar i opplæring for innvandrere"] -->|"Oppslag"| E["Prøvingsdato"]
  G["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"] -->|"Oppslag"| E["Prøvingsdato"]
  H["Deltar i høyere yrkesfaglig utdanning"] -->|"Oppslag"| E["Prøvingsdato"]
  I["Deltar i høyere utdanning"] -->|"Oppslag"| E["Prøvingsdato"]
  J["Deltar på kurs mv"] -->|"Oppslag"| E["Prøvingsdato"]
  K["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| D["Deltar i arbeidsmarkedstiltak"]
  K["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| F["Deltar i opplæring for innvandrere"]
  K["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| G["Deltar i grunnskoleopplæring, videregående opplæring og opplæring i grunnleggende ferdigheter"]
  K["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| H["Deltar i høyere yrkesfaglig utdanning"]
  K["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| I["Deltar i høyere utdanning"]
  K["Godkjent unntak for utdanning eller opplæring?"] -->|"EnAv"| J["Deltar på kurs mv"]
  L["Har svart ja på spørsmål om utdanning eller opplæring"] -->|"ErSann"| A["Brukeren er under utdanning eller opplæring"]
  M["Har svart nei på spørsmål om utdanning eller opplæring"] -->|"ErUsann"| A["Brukeren er under utdanning eller opplæring"]
  N["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| L["Har svart ja på spørsmål om utdanning eller opplæring"]
  N["Oppfyller kravet på unntak for utdanning eller opplæring"] -->|"Alle"| K["Godkjent unntak for utdanning eller opplæring?"]
  O["Krav til utdanning eller opplæring"] -->|"EnAv"| N["Oppfyller kravet på unntak for utdanning eller opplæring"]
  O["Krav til utdanning eller opplæring"] -->|"EnAv"| M["Har svart nei på spørsmål om utdanning eller opplæring"]
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