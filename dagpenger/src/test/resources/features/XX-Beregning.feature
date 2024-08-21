#language: no
@dokumentasjon @regel-beregning
Egenskap: BEREGNING

  Bakgrunn:
    Gitt at terskel er satt til 0,5

  Scenario: Jobbet under terskel og får penger
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Periode    | 52    | 01.01.2019 |          |
      | Sats       | 550   | 01.01.2020 |          |
      | FVA        | 37.5  | 01.01.2020 |          |
      | Sats       | 5555  | 04.01.2020 |          |
    Når meldekort for periode som begynner fra og med 01.01.2020 mottas med
      | Dag     | type   | verdi |
      | Mandag  | Arbeid | 5     |
      | Tirsdag | Arbeid | 5     |
      | Onsdag  | Arbeid | 5     |
      | Torsdag | Arbeid | 5     |
      | Fredag  | Arbeid | 5     |
      | Lørdag  |        |       |
      | Søndag  | Arbeid | 2     |
      | Mandag  | Arbeid | 4     |
      | Tirsdag | Arbeid | 4     |
      | Onsdag  | Arbeid | 4     |
      | Torsdag | Arbeid | 4     |
      | Fredag  | Arbeid | 4     |
      | Lørdag  |        | 0     |
      | Søndag  |        | 0     |
    Så skal det utbetales 48106,66666666667 kroner

