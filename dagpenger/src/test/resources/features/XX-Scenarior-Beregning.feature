#language: no
# Fra excel: https://navno.sharepoint.com/:x:/r/sites/TeamDagpenger/Shared%20Documents/General/Scenarier%20vedtak/vedtak_scenarier.xlsx?d=w130fdd6b0ecc4cab8e03fbf66b70d0a4&csf=1&web=1&e=qXai4h&nav=MTVfezAwMDAwMDAwLTAwMDEtMDAwMC0wMDAwLTAwMDAwMDAwMDAwMH0
Egenskap: Beregning av meldekort - scenarioer fra Excel

  Scenario: 1 - Bruker har rett til dagpenger i hele meldeperioden
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 800   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  | Arbeidstimer | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 8000,0 kroner
    Og utbetales 800 kroner etter avrunding på dag 1 til 10
    Og det forbrukes 10 dager


  Scenario: 2 - Stønadsperioden opphører i meldeperioden (TODO: Simulerer "stans" med tom dato på periode)
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 | 15.01.2020 |
      | Sats       | 800   | 01.01.2020 |            |
      | FVA        | 40    | 01.01.2020 |            |
      | Egenandel  | 0     | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 5600,0 kroner
    Og utbetales 800 kroner etter avrunding på dag 1 til 7
    Og det forbrukes 7 dager


  Scenario: 3 - Bruker får innvilget dagpenger med ordinær egenandel tilsvarende 3 dagsatser
    ## TODO: Denne differensierer fra Excel, der egenandel blir fordelt flatt på 10 dager
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 800   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 2400  | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 5600,0 kroner
    Og det forbrukes 2400 i egenandel
    Og utbetales 560 kroner etter avrunding på dag 1 til 10
    Og det forbrukes 10 dager

  Scenario: 4a - Bruker har vært syk 4 dager i uke i og arbeider lørdag og søndag i uke 1.
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 800   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 0     | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Sykdom       | 0     |
      | Tirsdag | Sykdom       | 0     |
      | Onsdag  | Sykdom       | 0     |
      | Torsdag | Sykdom       | 0     |
      | Fredag  |              | 0     |
      | Lørdag  | Arbeidstimer | 8     |
      | Søndag  | Arbeidstimer | 8     |
      | Mandag  |              | 0     |
      | Tirsdag |              | 0     |
      | Onsdag  |              | 0     |
      | Torsdag |              | 0     |
      | Fredag  |              | 0     |
      | Lørdag  | Arbeidstimer | 8     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 2400,0 kroner
    Og det forbrukes 6 dager

  @wip
  Scenario: 4b - Bruker har vært syk 4 dager i uke i og arbeider lørdag og søndag i uke 2.
    ## gal: 4b i excel-arket går over årskifte. Usikker/husker ikke hva som er forventet her.


  Scenario: 6 - Bruker får innvilget dagpenger med ordinær egenandel tilsvarende 3 dagsatser, fra første dag i meldeperioden. Stønaden stanses i meldeperioden.
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed   |
      | Terskel    | 50.0  |            |            |
      | Periode    | 52    | 01.01.2020 | 15.01.2020 |
      | Sats       | 800   | 01.01.2020 |            |
      | FVA        | 40    | 01.01.2020 |            |
      | Egenandel  | 2400  | 01.01.2020 |            |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
      | Mandag  | Arbeidstimer | 0     |
      | Tirsdag | Arbeidstimer | 0     |
      | Onsdag  | Arbeidstimer | 0     |
      | Torsdag | Arbeidstimer | 0     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 3200,0 kroner
    Og det trekkes "342.857142857142857136" kroner i egenandel på dag 1
    ## TODO: egenandel skal være 2400. Hvordan skal det rundes?
    Og det forbrukes 2399 i egenandel
    Og gjenstår 1 i egenandel
    ## TODO: Hva skjer med gjenstående egenandel?
    Og utbetales 457 kroner etter avrunding på dag 1
    Og utbetales 457 kroner etter avrunding på dag 2
    Og utbetales 457 kroner etter avrunding på dag 3
    Og utbetales 457 kroner etter avrunding på dag 4
    Og utbetales 457 kroner etter avrunding på dag 5
    Og utbetales 457 kroner etter avrunding på dag 6
    Og utbetales 458 kroner etter avrunding på dag 7
    Og det forbrukes 7 dager

  Scenario: 7 - Bruker får innvilget dagpenger med ordinær egenandel tilsvarende 3 dagsatser, fra første dag i meldeperioden.
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 01.01.2020 |          |
      | Sats       | 800   | 01.01.2020 |          |
      | FVA        | 40    | 01.01.2020 |          |
      | Egenandel  | 2400  | 01.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 5     |
      | Tirsdag | Arbeidstimer | 5     |
      | Onsdag  | Arbeidstimer | 2     |
      | Torsdag |              | 0     |
      | Fredag  | Arbeidstimer | 4     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
      | Mandag  |              | 0     |
      | Tirsdag |              | 0     |
      | Onsdag  |              | 0     |
      | Torsdag |              | 0     |
      | Fredag  |              | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 4000,0 kroner
    Og det forbrukes 2400 i egenandel
    Og det forbrukes 10 dager

  Scenario: 9 - Bruker får innvilget dagpenger fra fredag i uke 1 med ordinær egenandel på 3 dagsatser. Beregningsperioden er ulik meldeperioden. Bruker avtjener egenandelen i løpet av 2 hele og 2 halve dager.
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 10.01.2020 |          |
      | Sats       | 800   | 10.01.2020 |          |
      | FVA        | 40    | 10.01.2020 |          |
      | Egenandel  | 2400  | 10.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 8     |
      | Tirsdag | Arbeidstimer | 8     |
      | Onsdag  | Arbeidstimer | 8     |
      | Torsdag | Arbeidstimer | 8     |
      | Fredag  | Arbeidstimer | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
      | Mandag  |              | 0     |
      | Tirsdag | Arbeidstimer | 4     |
      | Onsdag  | Arbeidstimer | 3     |
      | Torsdag |              | 0     |
      | Fredag  |              | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 1700,0 kroner
    Og det forbrukes 2400 i egenandel
    Og det forbrukes 6 dager

  Scenario: 9 - Bruker får innvilget dagpenger fra fredag i uke 1 med ordinær ordinær egenandel på 3 dagsatser.
  Beregningsperioden er ulik meldeperioden. Bruker jobber under terskelverdi.
  Bruker betaler egenandelen på første meldekort, men jobber også på lørdag/søndag (ikke stønadsdager) og jobber over 8 timer på en stønadsdag .
  Siste dag egenandel trekkes, gjenstår 4 timer for utbetaling.
    Gitt at mottaker har vedtak med
      | Opplysning | verdi | fraOgMed   | tilOgMed |
      | Terskel    | 50.0  |            |          |
      | Periode    | 52    | 10.01.2020 |          |
      | Sats       | 800   | 10.01.2020 |          |
      | FVA        | 40    | 10.01.2020 |          |
      | Egenandel  | 2400  | 10.01.2020 |          |
    Når meldekort for periode som begynner fra og med 06.01.2020 mottas med
      | Dag     | type         | verdi |
      | Mandag  | Arbeidstimer | 8     |
      | Tirsdag | Arbeidstimer | 8     |
      | Onsdag  | Arbeidstimer | 8     |
      | Torsdag | Arbeidstimer | 8     |
      | Fredag  | Arbeidstimer | 4     |
      | Lørdag  | Arbeidstimer | 2     |
      | Søndag  | Arbeidstimer | 2     |
      | Mandag  | Arbeidstimer | 10    |
      | Tirsdag | Arbeidstimer | 2     |
      | Onsdag  |              | 0     |
      | Torsdag |              | 0     |
      | Fredag  |              | 0     |
      | Lørdag  |              | 0     |
      | Søndag  |              | 0     |
    Så skal kravet til tapt arbeidstid være oppfylt
    Og utbetales 400,0 kroner
    Og det forbrukes 2400 i egenandel
    Og det forbrukes 6 dager

    @wip
      ## Todo: Vi håndterer ikke sanksjon
  Scenario: 11 - Bruker får innvilget dagpenger fra fredag i uke 1 med forlenget ventetid jfr. §4-10. For eksemplets skyld er antall uker ventetid satt til 2, selv om det ikke er et reelt antall uker.
  Beregningsperioden er ulik meldeperioden.
  Bruker jobber under terskelverdi og avtjener sanksjonen i løpet av den tredje uka med stønad, forutsatt at ingen dager uten rett til utbetaling grunnet syksom eller annet fravær.
  OBS: Ved forlenget ventetid, skal det ikke gis ordinær ventetid.

  @wip
       ## Todo: Vi håndterer ikke sanksjon
  Scenario: 12 - Bruker får innvilget dagpenger fra fredag i uke 1 med forlenget ventetid jfr. §4-10. For eksemplets skyld er antall uker forlenget ventetid satt til 2, selv om det ikke er et reelt antall uker.
  Bruker har 1 sykedag. Dager med sykdom og fravær skal ikke inngå i meldeperioden. De skal dermed ikke inngå i terskelberegningen og man skal heller ikke kunne avtjene sanksjon på slike dager.
  For 1. meldekort er beregningsperioden ulik meldeperioden. Beregningsperioden har bare 5 arbeidsdager.
  For 2. meldekort er beregningsperioden = meldeperioden.
  Bruker avtjener her sanksjonen i løpet av uke 3 (2. meldekort).
  OBS: Ved forlenget ventetid, skal det ikke gis ordinær ventetid.

