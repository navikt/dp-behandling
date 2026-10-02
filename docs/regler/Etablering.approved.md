# Etablering

## Regeltre

```mermaid
graph RL
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"AlleMedGyldighetsperiodeFra"| B["Ny virksomhet"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"AlleMedGyldighetsperiodeFra"| C["Antas å føre til selvforsørgelse"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"AlleMedGyldighetsperiodeFra"| D["Godkjent næringsfaglig vurdering"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"AlleMedGyldighetsperiodeFra"| E["Egen Virksomhet"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"AlleMedGyldighetsperiodeFra"| F["Ikke selvforskyldt arbeidsledig"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"AlleMedGyldighetsperiodeFra"| G["Over 50 prosent eierandel i virksomheten"]
  A["Oppfyller vilkårene til etablering av egen virksomhet"] -->|"AlleMedGyldighetsperiodeFra"| H["Siste dato for dagpenger under etablering"]
```

## Akseptansetester

```gherkin
#language: no
@dokumentasjon @regel-etablering
Egenskap: Etablering

  Etablering påvirker bare sluttdatoen når vilkåret er relevant og godkjent.
  Da får siste periode med løpende rett samme til-og-med-dato som etableringsvilkåret.
  Fra-og-med-datoen endres ikke, og etablering overstyrer ikke en senere stans.
  Et negativt etableringsutfall endrer ikke eksisterende rett.

  Scenariomal: Saksbehandler vurderer opplysninger om etablering
    Gitt at etablering skal vurderes
    Og de øvrige vilkårene for etablering er oppfylt
    Og saksbehandler vurderer at det er en ny virksomhet "<nyVirksomhet>"
    Og saksbehandler vurderer selvforsørgelse som "<selvforsørget>"
    Og saksbehandler vurderer at virksomheten er egen "<egenVirksomhet>"
    Og saksbehandler vurderer at eierandelen er over femti prosent "<eierandelOverFemtiProsent>"
    Så skal vilkåret om etablering være "<utfall>"
    Og skal retten til dagpenger være "<harRett>"

    Eksempler:
      | nyVirksomhet | selvforsørget | egenVirksomhet | eierandelOverFemtiProsent | utfall | harRett |
      | Ja           | Ja            | Ja             | Ja                        | Ja     | Ja      |
      | Ja           | Ja            | Nei            | Ja                        | Nei    | Ja      |
      | Ja           | Nei           | Ja             | Ja                        | Nei    | Ja      |
      | Ja           | Nei           | Nei            | Ja                        | Nei    | Ja      |
      | Nei          | Ja            | Ja             | Ja                        | Nei    | Ja      |
      | Nei          | Ja            | Nei            | Ja                        | Nei    | Ja      |
      | Nei          | Nei           | Ja             | Ja                        | Nei    | Ja      |
      | Nei          | Nei           | Nei            | Ja                        | Nei    | Ja      |


  Scenario: Etablering skal ikke vurderes
    Gitt at etablering ikke skal vurderes
    Så skal opplysninger om etablering ikke være satt
    Og skal retten til dagpenger være "Ja"
``` 