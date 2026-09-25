# Etablering

## Regeltre

```mermaid
graph RL
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"Alle"| B["Ny virksomhet"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"Alle"| C["Antas å føre til selvforsørgelse"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"Alle"| D["Godkjent næringsfaglig vurdering"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"Alle"| E["Egen Virksomhet"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"Alle"| F["Ikke selvforskyldt arbeidsledig"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-etablering
Egenskap: Etablering

  Scenariomal: Saksbehandler vurderer opplysninger om etablering
    Gitt at etablering skal vurderes
    Og de øvrige vilkårene for etablering er oppfylt
    Og saksbehandler vurderer at etablering påvirker resultatet "<påvirkerResultat>"
    Og saksbehandler vurderer at det er en ny virksomhet "<nyVirksomhet>"
    Og saksbehandler vurderer selvforsørgelse som "<selvforsørget>"
    Og saksbehandler vurderer at virksomheten er egen "<egenVirksomhet>"
    Så skal vilkåret om etablering være "<utfall>"
    Og skal retten til dagpenger være "<harRett>"

    Eksempler:
      | påvirkerResultat | nyVirksomhet | selvforsørget | egenVirksomhet | utfall | harRett |
      | Ja               | Ja           | Ja             | Ja             | Ja     | Ja      |
      | Ja               | Ja           | Ja             | Nei            | Nei    | Nei     |
      | Ja               | Ja           | Nei            | Ja             | Nei    | Nei     |
      | Ja               | Ja           | Nei            | Nei            | Nei    | Nei     |
      | Ja               | Nei          | Ja             | Ja             | Nei    | Nei     |
      | Ja               | Nei          | Ja             | Nei            | Nei    | Nei     |
      | Ja               | Nei          | Nei            | Ja             | Nei    | Nei     |
      | Ja               | Nei          | Nei            | Nei            | Nei    | Nei     |
      | Nei              | Ja           | Ja             | Ja             | Ja     | Ja      |
      | Nei              | Ja           | Ja             | Nei            | Nei    | Ja      |
      | Nei              | Ja           | Nei            | Ja             | Nei    | Ja      |
      | Nei              | Ja           | Nei            | Nei            | Nei    | Ja      |
      | Nei              | Nei          | Ja             | Ja             | Nei    | Ja      |
      | Nei              | Nei          | Ja             | Nei            | Nei    | Ja      |
      | Nei              | Nei          | Nei            | Ja             | Nei    | Ja      |
      | Nei              | Nei          | Nei            | Nei            | Nei    | Ja      |

  Scenario: Etablering skal ikke vurderes
    Gitt at etablering ikke skal vurderes
    Så skal opplysninger om etablering ikke være satt
    Og skal retten til dagpenger være "Ja"
``` 