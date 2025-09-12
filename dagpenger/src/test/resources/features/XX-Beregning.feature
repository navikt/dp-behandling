#language: no
Egenskap: Beregning av meldekort

  Scenario: Jobbet over terskel og får ingen utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 550   | 01.01.2020 | 12.01.2020 |
      | FVA        | 37.5  | 01.01.2020 |            |
      | Sats       | 5555  | 13.01.2020 |            |
      | Egenandel  | 0     | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 5     |
      | Lørdag  |              |       |
      | Søndag  | Arbeidstimer | 2     |
      | Mandag  | Arbeidstimer | 4     |
      | Tirsdag | Arbeidstimer | 4     |
      | Onsdag  | Arbeidstimer | 4     |
      | Torsdag | Arbeidstimer | 4     |
      | Fredag  | Arbeidstimer | 4     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid ikke være oppfylt

  Scenario: Jobbet nøyaktig på terskel og får 50% gradert utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 5     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 500,0 kroner

  Scenario: Jobbet under terskel og får 25% gradert utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 101   | 01.01.2020 |          |
      | FVA        | 37.5  | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 2.5   |
      | Torsdag | Arbeidstimer | 4     |
      | Fredag  | Arbeidstimer | 2.5   |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 754 kroner
    Og det forbrukes 10 dager
    Og utbetales 75 kroner etter avrunding på dag 1
    Og utbetales 75 kroner etter avrunding på dag 2
    Og utbetales 75 kroner etter avrunding på dag 3
    Og utbetales 75 kroner etter avrunding på dag 4
    Og utbetales 75 kroner etter avrunding på dag 5
    Og utbetales 75 kroner etter avrunding på dag 6
    Og utbetales 75 kroner etter avrunding på dag 7
    Og utbetales 75 kroner etter avrunding på dag 8
    Og utbetales 75 kroner etter avrunding på dag 9
    Og utbetales 79 kroner etter avrunding på dag 10

  Scenario: Jobbet under terskel og fordeler desimaltall overskytende riktig
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 903   | 01.01.2020 |          |
      | FVA        | 37.5  | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 3     |
      | Tirsdag | Arbeidstimer | 2     |
      | Onsdag  | Arbeidstimer | 2     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 8187 kroner
    Og det forbrukes 10 dager
    Og utbetales 818 kroner etter avrunding på dag 1
    Og utbetales 825 kroner etter avrunding på dag 10

  Scenario: Jobbet under terskel og får 50% gradert utbetaling med endring av sats midt i perioden
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 100   | 01.01.2020 | 12.01.2020 |
      | FVA        | 40    | 01.01.2020 |            |
      | Sats       | 200   | 13.01.2020 |            |
      | Egenandel  | 0     | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 750,0 kroner
    # 250 kroner for første uke
    # 500 kroner for andre uke

  Scenario: Jobbet under terskel med sykdom og fravær og får 100% utbetaling for arbeidsdagene
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Fravær       |       |
      | Tirsdag | Fravær       |       |
      | Onsdag  | Fravær       |       |
      | Torsdag | Fravær       |       |
      | Fredag  | Sykdom       |       |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 500,0 kroner
    Og det forbrukes 5 dager
    Og det gjenstår 255 dager

  Scenario: Jobbet over terskel med sykdom og fravær og får ikke utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 6     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Fravær       |       |
      | Tirsdag | Fravær       |       |
      | Onsdag  | Fravær       |       |
      | Torsdag | Fravær       |       |
      | Fredag  | Sykdom       |       |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid ikke være oppfylt
    Og det forbrukes 0 dager

  Scenario: Endring av FVA midt i perioden
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 100   | 01.01.2020 |            |
      | FVA        | 20    | 01.01.2020 | 12.01.2020 |
      | FVA        | 40    | 13.01.2020 |            |
      | Egenandel  | 0     | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 5     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 500,0 kroner

  Scenario: Endring av terskel midt i perioden, overgang fra permittering til ordinær
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 40.0  |            | 12.01.2020 |
      | Terskel    | 50.0  | 13.01.2020 |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 100   | 01.01.2020 |            |
      | FVA        | 40    | 01.01.2020 |            |
      | Egenandel  | 0     | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 2     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 475,0 kroner

  Scenario: Oppstart av vedtak midt i perioden
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  | 10.01.2020 |          |
      | Periode    | 52    | 10.01.2020 |          |
      | Sats       | 100   | 10.01.2020 |          |
      | FVA        | 20    | 10.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 10    |
      | Tirsdag | Arbeidstimer | 10    |
      | Onsdag  | Arbeidstimer | 10    |
      | Torsdag | Arbeidstimer | 10    |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 600,0 kroner

  Scenario: Jobbet under terskel og skal forbruke siste rest av perioden
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 1     | 01.01.2019 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 5     |
      | Torsdag | Arbeidstimer | 5     |
      | Fredag  | Arbeidstimer | 5     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 250,0 kroner
    Og det forbrukes 5 dager
    Og det gjenstår 0 dager


#  Scenario: Jobbet under terskel og skal forbruke siste rest av perioden men har vært syk
#    Gitt at mottaker har vedtak med
#      | Opplysning | verdi | fraOgMed   | tilOgMed |
#      | Terskel    | 50.0   |            |          |
#      | Periode    | 1     | 01.01.2019 |          |
#      | Sats       | 100   | 01.01.2020 |          |
#      | FVA        | 40    | 01.01.2020 |          |
#      | Egenandel  | 0     | 01.01.2020 |          |
#    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
#      | Dag     | type         | verdi |
#      | Mandag  | Fravær       |       |
#      | Tirsdag | Arbeidstimer | 0     |
#      | Onsdag  | Arbeidstimer | 0     |
#      | Torsdag | Arbeidstimer | 0     |
#      | Fredag  | Arbeidstimer | 0     |
#      | Lørdag  | Arbeidstimer | 0     |
#      | Søndag  | Arbeidstimer | 0     |
#      | Mandag  | Arbeidstimer | 0     |
#      | Tirsdag | Arbeidstimer | 5     |
#      | Onsdag  | Arbeidstimer | 5     |
#      | Torsdag | Arbeidstimer | 5     |
#      | Fredag  | Arbeidstimer | 5     |
#      | Lørdag  | Arbeidstimer | 0     |
#      | Søndag  | Arbeidstimer | 0     |
#    Så skal kravet til tapt arbeidstid være oppfylt
#    Og utbetales 500,0 kroner
#    Og det forbrukes 5 dager
#    Og det gjenstår 0 dager

  Scenario: Skal få 50% gradert utbetaling, men trekkes for egenandel
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 300   | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 10 dager
    Og det forbrukes 300 i egenandel
    Og gjenstår 0 i egenandel
    Og utbetales 700,0 kroner

  Scenario: Oppstart midt i perioden, med 25% gradert utbetaling, men trekkes for egenandel
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 16.01.2020 |          |
      | Sats       | 100   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 300   | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 4     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 2 dager
    Og det forbrukes 150 i egenandel
    Og gjenstår 150 i egenandel
    Og utbetales 0,0 kroner

  Scenario: Går fra høy til lav sats og forbruker all egenandel
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 1000  | 01.01.2020 | 12.01.2020 |
      | Sats       | 100   | 13.01.2020 |            |
      | FVA        | 40    | 01.01.2020 |            |
      | Egenandel  | 3000  | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 10 dager
    Og det forbrukes 3000 i egenandel
    Og gjenstår 0 i egenandel
    Og utbetales 2500,0 kroner
    Og utbetales 454 kroner på dag 1
    Og utbetales 457 kroner på dag 5
    Og utbetales 45 kroner på dag 6
    Og utbetales 47 kroner på dag 10

  Scenario: Har for lav sats til å få utbetaling og bruker ikke opp all egenandel
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 16.01.2020 |          |
      | Sats       | 1000  | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 3000  | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 2 dager
    Og det forbrukes 2000 i egenandel
    Og gjenstår 1000 i egenandel
    Og utbetales 0,0 kroner

  Scenario: Kan ikke bruke mer av egenandel enn gradert utbetaling
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 500   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 3000  | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 4     |
      | Tirsdag | Arbeidstimer | 4     |
      | Onsdag  | Arbeidstimer | 4     |
      | Torsdag | Arbeidstimer | 4     |
      | Fredag  | Arbeidstimer | 4     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 4     |
      | Tirsdag | Arbeidstimer | 4     |
      | Onsdag  | Arbeidstimer | 4     |
      | Torsdag | Arbeidstimer | 4     |
      | Fredag  | Arbeidstimer | 4     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 10 dager
    Og det forbrukes 2500 i egenandel
    Og gjenstår 500 i egenandel
    Og utbetales 0,0 kroner

  Scenario: Eksempel 3 - Eksempel med satsendring, endringsvedtak.
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 |            |
      | Sats       | 800   | 01.01.2020 | 07.01.2020 |
      | Sats       | 1200  | 08.01.2020 |            |
      | FVA        | 40    | 01.01.2020 |            |
      | Egenandel  | 2400  | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 0     |
      | Søndag  | Arbeidstimer | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og det forbrukes 10 dager
    Og det forbrukes 2400 i egenandel
    Og gjenstår 0 i egenandel
    Og utbetales 8800 kroner


  Scenario: Jobbet over terskel i helg
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 1200  | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 20    |
      | Søndag  | Arbeidstimer | 20    |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  | Arbeidstimer | 20    |
      | Søndag  | Arbeidstimer | 20    |
    Så skal kravet til tapt arbeidstid ikke være oppfylt
    Og utbetales 0 kroner
    Og det forbrukes 0 dager


  Scenario: Kombinerer aktiviteter på samme dag
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 1200  | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type                    | verdi |
      | Mandag  | Arbeidstimer, Utdanning | 0     |
      | Tirsdag | Utdanning, Fravær       | 0     |
      | Onsdag  | Arbeidstimer            | 0     |
      | Torsdag | Arbeidstimer            | 0     |
      | Fredag  | Arbeidstimer            | 0     |
      | Lørdag  | Arbeidstimer            | 0     |
      | Søndag  | Arbeidstimer            | 0     |
      | Mandag  | Arbeidstimer            | 0     |
      | Tirsdag | Arbeidstimer            | 0     |
      | Onsdag  | Arbeidstimer            | 0     |
      | Torsdag | Arbeidstimer            | 0     |
      | Fredag  | Arbeidstimer            | 0     |
      | Lørdag  | Arbeidstimer            | 0     |
      | Søndag  | Arbeidstimer            | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 10800 kroner
    Og det forbrukes 9 dager

