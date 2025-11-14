#language: no
Egenskap: § 4-8 4.avsnitt Beregning av bortfall ved for sen melding

  Scenario: Har sendt meldekort for sent
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 550   | 01.01.2020 | 12.01.2020 |
      | FVA        | 37.5  | 01.01.2020 |            |
      | Sats       | 5555  | 13.01.2020 |            |
      | Egenandel  | 0     | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi | meldt  |
      | Mandag  | Arbeidstimer | 0     |  Nei   |
      | Tirsdag | Arbeidstimer | 0     |  Nei   |
      | Onsdag  | Arbeidstimer | 0     |  Nei   |
      | Torsdag | Arbeidstimer | 0     |  Nei   |
      | Fredag  | Arbeidstimer | 0     |  Nei   |
      | Lørdag  |              |       |  Nei   |
      | Søndag  | Arbeidstimer | 0     |  Nei   |
      | Mandag  | Arbeidstimer | 0     |  Nei   |
      | Tirsdag | Arbeidstimer | 0     |  Ja    |
      | Onsdag  | Arbeidstimer | 0     |  Ja    |
      | Torsdag | Arbeidstimer | 0     |  Ja    |
      | Fredag  | Arbeidstimer | 0     |  Ja    |
      | Lørdag  |              | 0     |  Ja    |
      | Søndag  |              | 0     |  Ja    |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 4 dager

  Scenario: Har sendt meldekort akkurat i tide
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 550   | 01.01.2020 | 12.01.2020 |
      | FVA        | 37.5  | 01.01.2020 |            |
      | Sats       | 5555  | 13.01.2020 |            |
      | Egenandel  | 0     | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi | meldt  |
      | Mandag  | Arbeidstimer | 0     |  Nei   |
      | Tirsdag | Arbeidstimer | 0     |  Nei   |
      | Onsdag  | Arbeidstimer | 0     |  Nei   |
      | Torsdag | Arbeidstimer | 0     |  Nei   |
      | Fredag  | Arbeidstimer | 0     |  Nei   |
      | Lørdag  |              |       |  Nei   |
      | Søndag  | Arbeidstimer | 0     |  Nei   |
      | Mandag  | Arbeidstimer | 0     |  Ja    |
      | Tirsdag | Arbeidstimer | 0     |  Ja    |
      | Onsdag  | Arbeidstimer | 0     |  Ja    |
      | Torsdag | Arbeidstimer | 0     |  Ja    |
      | Fredag  | Arbeidstimer | 0     |  Ja    |
      | Lørdag  |              | 0     |  Ja    |
      | Søndag  |              | 0     |  Ja    |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 10 dager

  Scenario: Jobbet over terskel på dager som ikke er meldt har ikke noe betydning
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 550   | 01.01.2020 | 12.01.2020 |
      | FVA        | 37.5  | 01.01.2020 |            |
      | Sats       | 5555  | 13.01.2020 |            |
      | Egenandel  | 0     | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi | meldt  |
      | Mandag  | Arbeidstimer | 8     |  Nei   |
      | Tirsdag | Arbeidstimer | 8     |  Nei   |
      | Onsdag  | Arbeidstimer | 8     |  Nei   |
      | Torsdag | Arbeidstimer | 8     |  Nei   |
      | Fredag  | Arbeidstimer | 8     |  Nei   |
      | Lørdag  |              |       |  Nei   |
      | Søndag  | Arbeidstimer | 8     |  Nei   |
      | Mandag  | Arbeidstimer | 8     |  Nei   |
      | Tirsdag | Arbeidstimer | 0     |  Ja    |
      | Onsdag  | Arbeidstimer | 0     |  Ja    |
      | Torsdag | Arbeidstimer | 0     |  Ja    |
      | Fredag  | Arbeidstimer | 0     |  Ja    |
      | Lørdag  |              | 0     |  Ja    |
      | Søndag  |              | 0     |  Ja    |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 4 dager