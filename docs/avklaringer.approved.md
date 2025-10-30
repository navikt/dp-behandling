# Avklaringer
Avklaringer opprettes hvor regelmotoren er usikker på enten fakta eller riktig vei videre.

Avklaringer opprettes av "kontrollpunkt" som gjør en vurdering av opplysninger og ser om det avklaringen er nødvendig.

Endringer i opplysninger vil automatisk lukke avklaringen om kontrollpunktet sier den ikke lengre er nødvendig.
Tilsvarende vil avklaringen åpnes opp igjen om opplysningene endres.

## Sjekk hvilke barn som skal gi barnetillegg
**Kode:** `BarnMåGodkjennes`

### Beskrivelse
Barn må godkjennes om de skal gi barnetillegg

### Tilknyttet regelsett
- § 4-12. Dagpengenes størrelse
---
## Sjekk om beregnet arbeidstid er korrekt
**Kode:** `BeregnetArbeidstid`

### Beskrivelse
Du må sjekke om beregnet vanlig arbeidstid er korrekt og at det er brukt riktig beregningsregel. <br>Du må også sjekke om ny vanlig arbeidstid er korrekt

### Tilknyttet regelsett
- § 4-3. Tap av arbeidsinntekt og arbeidstid
---
## Bruker har oppgitt bostedsland som ikke er Norge
**Kode:** `Bostedsland`

### Beskrivelse
Du må sjekke om bruker oppfyller vilkåret om opphold i Norge eller er unntatt fra vilkåret om opphold

### Tilknyttet regelsett
- § 4-2. Opphold i Norge
---
## Bruker er under 18
**Kode:** `BrukerUnder18`

### Beskrivelse
Bruker er under 18 og skal ikke ha automatisk behandling

---
## Sjekk om arbeid i EØS fører til sammenlegging
**Kode:** `EØSArbeid`

### Beskrivelse
Personen har oppgitt arbeid fra EØS i søknaden. Det må vurderes om det skal være sammenlegging.

---
## Sjekk om søker har andre fulle ytelser
**Kode:** `FulleYtelser`

### Beskrivelse
Om søker har andre fulle ytelser må det velges mellom dagpenger eller disse ytelsene

### Tilknyttet regelsett
- § 4-24. Medlem som har fulle ytelser etter folketrygdloven eller avtalefestet pensjon
---
## !! Behandles som gjenopptak i ny løsning. Disse støtter vi ikke, så IKKE RØR 😬
**Kode:** `GjenopptakBehandling`

### Beskrivelse
Denne saken har en innvilget behandling i ny løsning, og det må vurderes om den skal gjenopptas.

---
## Sjekk om bruker skal ha dagpenger som permittert
**Kode:** `HarOppgittPermittering`

### Beskrivelse
Du må vurdere om bruker er permittert og oppfyller kravene til permittering

### Tilknyttet regelsett
- § 4-7. Dagpenger til permitterte
---
## Sjekk om bruker skal ha dagpenger som permittert fra fiskeindustrien
**Kode:** `HarOppgittPermitteringFiskeindustri`

### Beskrivelse
Du må vurdere om bruker er permittert og oppfyller kravene til permittering fra fiskeindustrien

### Tilknyttet regelsett
- § 6-7. Permittering i fiskeforedlingsindustrien, sjømatindustrien og fiskeoljeindustrien
---
## Sjekk hva bruker har oppgitt som tilleggsopplysninger i søknaden
**Kode:** `HarTilleggsopplysninger`

### Beskrivelse
Du må vurdere om tilleggsopplysninger har betydning for saken

---
## Sjekk om nylig lukkede saker i Arena kan påvirke behandlingen
**Kode:** `HattLukkedeSakerSiste8Uker`

### Beskrivelse
Personen har lukkede saker i Arena siste 8 uker. Har vi nylig gitt avslag bør vi sjekke om det er nødvendig med ekstra <br>veiledning.

---
## Søker er ikke registrert som arbeidssøker
**Kode:** `IkkeRegistrertSomArbeidsøker`

### Beskrivelse
Søker er ikke registrert som arbeidssøker.

### Tilknyttet regelsett
- § 4-5. Reelle arbeidssøkere
---
## Sjekk om inntekt for neste måned er relevant
**Kode:** `InntektNesteKalendermåned`

### Beskrivelse
Personen har inntekt som tilhører neste inntektsperiode. Vurder om det er tilstrekkelige inntekter til at utfallet vil <br>endre seg i neste inntektsperiode.

### Tilknyttet regelsett
- § 4-4. Krav til minsteinntekt
---
## Sjekk om arbeid utenfor Norge påvirker retten til dagpenger
**Kode:** `JobbetUtenforNorge`

### Beskrivelse
Personen har oppgitt arbeid utenfor Norge i søknaden. Sjekk om arbeidsforholdene som er oppgitt i søknaden skal være <br>med i vurderingen av retten til dagpenger.

---
## Vurderingen av reell arbeidssøker mangler og utfallet er innvilgelse
**Kode:** `ManglerReellArbeidssøker`

### Beskrivelse
Du må sørge for at vurderingen av reell arbeidssøker er utført før du kan innvilge 

---
## Sjekk om det er sak som kan gjenopptas i Arena
**Kode:** `MuligGjenopptak`

### Beskrivelse
Personen har åpne saker i Arena som kan være gjenopptak. Saker som skal gjenopptas må håndteres i Arena.

---
## Grunnbeløpet for dagpengegrunnlag kan være utdatert
**Kode:** `NyttGrunnbeløpForGrunnlag`

### Beskrivelse
Prøvingsdatoen er 1. mai eller senere. Grunnbeløpet for inneværende år var ikke iverksatt på behandlingstidspunktet.<br>Hvis grunnbeløpet ikke er vedtatt enda kan behandlingen godkjennes med det gamle grunnbeløpet. Det blir G-justert i Arena.<br>Er grunnbeløpet for inneværende år vedtatt, kjør behandlingen på nytt og huk av 'Grunnbeløp for grunnlag' for å oppdatere grunnbeløpet.

### Tilknyttet regelsett
- § 4-11. Dagpengegrunnlag
---
## Sjekk om behandlingen bør ventes til etter A-ordningens rapporteringsfrist
**Kode:** `PrøvingsdatoEtterRapporteringsfrist`

### Beskrivelse
Prøvingsdatoen er innenfor neste rapporteringsperiode for inntekt. <br>Vurder om du bør vente til etter fristen for å få med korrekte inntekter.

---
## Sjekk om søker oppfyller unntak til å være reell arbeidssøker
**Kode:** `ReellArbeidssøkerUnntak`

### Beskrivelse
Det må vurderes om søker kvalifiserer til unntakene til reell arbeidssøker

### Tilknyttet regelsett
- § 4-5. Reelle arbeidssøkere
---
## Sjekk om det er andre ytelser fra folketrygden som skal samordnes
**Kode:** `Samordning`

### Beskrivelse
Vi har funnet andre ytelser fra folketrygden. Det må vurderes om, og eventuelt hvordan, de skal samordnes med dagpengene.

### Tilknyttet regelsett
- § 4-25. Samordning med reduserte ytelser fra folketrygden, eller redusert avtalefestet pensjon
---
## Sjekk om valgt prøvingsdato er riktig
**Kode:** `SjekkPrøvingsdato`

### Beskrivelse
Sjekk at valgt prøvingsdato er riktig

---
## Sjekk om søker har fått sykepenger på grunn av svangerskap som skal med i minsteinntekt
**Kode:** `SvangerskapsrelaterteSykepenger`

### Beskrivelse
Personen har fått utbetalt sykepenger. Om det er svangerskapsrelaterte sykepenger skal være med i inntektstgrunnlaget for <br>vurderingen av minste arbeidsinntekt.

### Tilknyttet regelsett
- § 4-4. Krav til minsteinntekt
---
## Søknadstidspunktet ligger for langt fram i tid
**Kode:** `SøknadstidspunktForLangtFramITid`

### Beskrivelse
Søknadstidspunktet ligger mer enn 14 dager fram i tid

---
## Bruker har søkt om gjenopptak
**Kode:** `SøktGjenopptak`

### Beskrivelse
Personen har søkt om gjenopptak. Saker som skal gjenopptas må håndteres i Arena.

---
## Velg kun en beregningsregel for tap av arbeidsinntekt og arbeidstid
**Kode:** `TapAvArbeidsinntektOgArbeidstid`

### Beskrivelse
Kun én beregningsregel kan være gyldig til en hver tid. <br>Velg en av Arbeidstid siste 6 måneder, Arbeidstid siste 12 måneder eller Arbeidstid siste 36 måneder.

### Tilknyttet regelsett
- § 4-3. Tap av arbeidsinntekt og arbeidstid
❌ Kan ikke kvitteres

---
## Sjekk om søker oppfyller vilkåret til dagpenger ved avtjent verneplikt
**Kode:** `Verneplikt`

### Beskrivelse
Søker har oppgitt at de har avtjent verneplikt. Det må sjekkes om kravet til dagpenger ved avtjent verneplikt er oppfylt.

### Tilknyttet regelsett
- § 4-19. Dagpenger etter avtjent verneplikt
---
## Virkningstidspunkt ligger for langt fram i tid
**Kode:** `VirkningstidspunktForLangtFramItid`

### Beskrivelse
Virkningstidspunkt ligger mer enn 14 dager fram i tid

### Tilknyttet regelsett
- § 3-1. Søknadstidspunkt
---
## Sjekk om det er ytelser utenfor folketrygden som skal samordnes
**Kode:** `YtelserUtenforFolketrygden`

### Beskrivelse
Sjekk hvilke ytelser som er oppgitt utenfor folketrygden og om de skal ha konsekvens for dagpengene

### Tilknyttet regelsett
- § 4-26. Samordning med ytelser utenfor folketrygden
---
