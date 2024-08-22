#language: no
@dokumentasjon @regel-beregning
Egenskap: BEREGNING

  Bakgrunn:
    Gitt at terskel er satt til 0,5

  Scenario: Jobbet over terskel og får ingen utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Periode    | 52    | 01.01.2019 |          |
      | Sats       | 550   | 01.01.2020 |          |
      | FVA        | 37.5  | 01.01.2020 |          |
      | Sats       | 5555  | 13.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
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
    Så skal kravet til tapt arbeidstid ikke være oppfylt

  Scenario: Jobbet nøyaktig på terskel og får 50% gradert utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Periode    | 52    | 01.01.2019 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type   | verdi |
      | Mandag  | Arbeid | 5     |
      | Tirsdag | Arbeid | 5     |
      | Onsdag  | Arbeid | 5     |
      | Torsdag | Arbeid | 5     |
      | Fredag  | Arbeid | 0     |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
      | Mandag  | Arbeid | 0     |
      | Tirsdag | Arbeid | 5     |
      | Onsdag  | Arbeid | 5     |
      | Torsdag | Arbeid | 5     |
      | Fredag  | Arbeid | 5     |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 500,0 kroner

  Scenario: Jobbet under terskel og får 25% gradert utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Periode    | 52    | 01.01.2019 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type   | verdi |
      | Mandag  | Arbeid | 5     |
      | Tirsdag | Arbeid | 5     |
      | Onsdag  | Arbeid | 5     |
      | Torsdag | Arbeid | 5     |
      | Fredag  | Arbeid | 0     |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
      | Mandag  | Arbeid | 0     |
      | Tirsdag | Arbeid | 0     |
      | Onsdag  | Arbeid | 0     |
      | Torsdag | Arbeid | 0     |
      | Fredag  | Arbeid | 0     |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 750,0 kroner

  Scenario: Jobbet under terskel og får 50% gradert utbetaling med endring av sats midt i perioden
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Periode    | 52    | 01.01.2019 |            |
      | Sats       | 100   | 01.01.2020 | 12.01.2020 |
      | FVA        | 40    | 01.01.2020 |            |
      | Sats       | 200   | 13.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type   | verdi |
      | Mandag  | Arbeid | 5     |
      | Tirsdag | Arbeid | 5     |
      | Onsdag  | Arbeid | 5     |
      | Torsdag | Arbeid | 5     |
      | Fredag  | Arbeid | 0     |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
      | Mandag  | Arbeid | 5     |
      | Tirsdag | Arbeid | 5     |
      | Onsdag  | Arbeid | 5     |
      | Torsdag | Arbeid | 5     |
      | Fredag  | Arbeid | 0     |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 750,0 kroner
    # 250 kroner for første uke
    # 500 kroner for andre uke

  Scenario: Jobbet under terskel med sykdom og fravær og får 100% utbetaling for arbeidsdagene
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Periode    | 52    | 01.01.2019 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type   | verdi |
      | Mandag  | Arbeid | 0     |
      | Tirsdag | Arbeid | 0     |
      | Onsdag  | Arbeid | 0     |
      | Torsdag | Arbeid | 0     |
      | Fredag  | Arbeid | 0     |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
      | Mandag  | Fravær |       |
      | Tirsdag | Fravær |       |
      | Onsdag  | Fravær |       |
      | Torsdag | Fravær |       |
      | Fredag  | Sykdom |       |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 500,0 kroner

  Scenario: Jobbet over terskel med sykdom og fravær og får ikke utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Periode    | 52    | 01.01.2019 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type   | verdi |
      | Mandag  | Arbeid | 5     |
      | Tirsdag | Arbeid | 5     |
      | Onsdag  | Arbeid | 5     |
      | Torsdag | Arbeid | 6     |
      | Fredag  | Arbeid | 0     |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
      | Mandag  | Fravær |       |
      | Tirsdag | Fravær |       |
      | Onsdag  | Fravær |       |
      | Torsdag | Fravær |       |
      | Fredag  | Sykdom |       |
      | Lørdag  | Arbeid | 0     |
      | Søndag  | Arbeid | 0     |
    Så skal kravet til tapt arbeidstid ikke være oppfylt
