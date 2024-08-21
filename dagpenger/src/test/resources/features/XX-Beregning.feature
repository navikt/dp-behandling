#language: no
@dokumentasjon @regel-utestengning
Egenskap: BEREGNING

  Scenario: Søker oppfyller kravet om å ikke være utestengt
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Periode    | 52    | 01.01.2019 |            |
      | Sats       | 5500  | 01.01.2020 |            |
      | Forbrukt   | true  | 02.01.2020 | 02.01.2020 |
      | Sats       | 5555  | 04.01.2020 |            |
    Når meldekort mottas med
      | Opplysning | verdi | fraOgMed   | tilOgMed  |
      | Arbeid     | 10    | 01.01.2020 | 01.01.202 |
      | Arbeid     | 10    | 02.01.2020 | 02.01.202 |
      | Arbeid     | 10    | 03.01.2020 | 03.01.202 |
      | Arbeid     | 10    | 04.01.2020 | 04.01.202 |
    Så skal det utbetales 5000 kroner

