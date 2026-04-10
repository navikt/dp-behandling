# Avklaringer
Avklaringer opprettes hvor regelmotoren er usikker på enten fakta eller riktig vei videre.

Avklaringer opprettes av "kontrollpunkt" som gjør en vurdering av opplysninger og ser om det avklaringen er nødvendig.

Endringer i opplysninger vil automatisk lukke avklaringen om kontrollpunktet sier den ikke lengre er nødvendig.
Tilsvarende vil avklaringen åpnes opp igjen om opplysningene endres.

## Barnetillegg
**Kode:** `BarnMåGodkjennes`

### Beskrivelse
Barn må godkjennes om de skal gi barnetillegg. Sjekk hvilke barn som skal gi barnetillegg.

### Tilknyttet regelsett
- [§ 4-12. Dagpengenes størrelse](./opplysninger.approved.md#-4-12-dagpengenes-størrelse)
---
## Arbeidstid
**Kode:** `BeregnetArbeidstid`

### Beskrivelse
Sjekk om vanlig arbeidstid er korrekt og at det er brukt riktig beregningsregel. <br>Du må også sjekke om ny vanlig arbeidstid er korrekt.

### Tilknyttet regelsett
- [§ 4-3. Tap av arbeidsinntekt og arbeidstid](./opplysninger.approved.md#-4-3-tap-av-arbeidsinntekt-og-arbeidstid)
---
## Bostedsland er ikke Norge
**Kode:** `Bostedsland`

### Beskrivelse
Søker har oppgitt bostedsland som ikke er i Norge.<br>Sjekk om bruker er unntatt vilkårene for opphold i Norge.

### Tilknyttet regelsett
- [§ 4-2. Opphold i Norge](./opplysninger.approved.md#-4-2-opphold-i-norge)
---
## Bruker er under 18 år
**Kode:** `BrukerUnder18`

### Beskrivelse
Søker er under 18 år og skal ikke ha automatisk behandling.

---
## Arbeid i EØS, Sveits eller Storbritannia
**Kode:** `EØSArbeid`

### Beskrivelse
Søker har oppgitt arbeid fra EØS, Sveits eller Storbritannia i søknaden. <br>Vurder om det skal være sammenlegging.

---
## Andre fulle ytelser etter folketrygdloven
**Kode:** `FulleYtelser`

### Beskrivelse
Sjekk om søker har andre fulle ytelser. <br>Om søker har andre fulle ytelser, må det velges mellom dagpenger eller disse ytelsene.

### Tilknyttet regelsett
- [§ 4-24. Medlem som har fulle ytelser etter folketrygdloven eller avtalefestet pensjon](./opplysninger.approved.md#-4-24-medlem-som-har-fulle-ytelser-etter-folketrygdloven-eller-avtalefestet-pensjon)
---
## !! Behandles som gjenopptak i ny løsning. Disse støtter vi ikke, så IKKE RØR 😬
**Kode:** `GjenopptakBehandling`

### Beskrivelse
Denne saken har en innvilget behandling i ny løsning. og det må vurderes om den skal gjenopptas.

---
## Permittering
**Kode:** `HarOppgittPermittering`

### Beskrivelse
Søker har oppgitt permittering i søknaden. Vurder om søker er permittert og har rett til dagpenger som permittert.

### Tilknyttet regelsett
- [§ 4-7. Dagpenger til permitterte](./opplysninger.approved.md#-4-7-dagpenger-til-permitterte)
---
## Permittering fiskeindustri
**Kode:** `HarOppgittPermitteringFiskeindustri`

### Beskrivelse
Søker har oppgitt permittering fra fiskeindustri i søknaden. <br>Vurder om søker er permittert og har rett til dagpenger som permittert fra fiskeindustrien.

### Tilknyttet regelsett
- [§ 6-7. Permittering i fiskeforedlingsindustrien, sjømatindustrien og fiskeoljeindustrien](./opplysninger.approved.md#-6-7-permittering-i-fiskeforedlingsindustrien,-sjømatindustrien-og-fiskeoljeindustrien)
---
## Omgjøring uten klage
**Kode:** `HarSvartPåOmgjøringUtenKlage`

### Beskrivelse
Saksbehandler må svare på hvorfor vedtaket omgjøres uten at det har vært klage på tidligere vedtak. Sjekk at det er lagt inn en begrunnelse for omgjøringen.

### Tilknyttet regelsett
- [§ 6-35. Omgjøring av vedtak uten klage](./opplysninger.approved.md#-6-35-omgjøring-av-vedtak-uten-klage)
❌ Kan ikke kvitteres

---
## Tilleggsopplysninger
**Kode:** `HarTilleggsopplysninger`

### Beskrivelse
Søker har oppgitt tilleggsopplysninger i søknaden. Vurder om tilleggsopplysninger har betydning for saken.<br>I tillegg bør det vurderes om opplysningene skal videreformidles til Nav lokal eller om søker trenger veiledning.

---
## Nylig lukkede saker i Arena
**Kode:** `HattLukkedeSakerSiste8Uker`

### Beskrivelse
Søker har lukkede saker i Arena fra de siste 8 ukene. Sjekk om disse kan påvirke behandlingen.<br>Hvis vi nylig har gitt avslag, sjekk om det er nødvendig med ekstra veiledning.

---
## Ikke oppfylt meldeplikt
**Kode:** `IkkeOppfyllerMeldeplikt`

### Beskrivelse
Bruker har ikke meldt seg i tide (§4-8). Vurder om dagpenger skal stanses.

---
## Ikke registrert som arbeidssøker
**Kode:** `IkkeRegistrertSomArbeidsøker`

### Beskrivelse
Søker er ikke registrert som arbeidssøker på søknadstidspunktet.

### Tilknyttet regelsett
- [§ 4-5. Reelle arbeidssøkere](./opplysninger.approved.md#-4-5-reelle-arbeidssøkere)
---
## Manuelt redigert inntekt
**Kode:** `InntektManueltRedigert`

### Beskrivelse
Inntekten er manuelt redigert. Du må begrunne årsaken.

---
## Inntekt neste måned
**Kode:** `InntektNesteKalendermåned`

### Beskrivelse
Søker har inntekt som tilhører neste kalendermåned. <br>Sjekk om det er tilstrekkelige inntekter til at utfallet eller dagpengegrunnlaget vil endre seg i neste kalendermåned.

### Tilknyttet regelsett
- [§ 4-4. Krav til minsteinntekt](./opplysninger.approved.md#-4-4-krav-til-minsteinntekt)
---
## Arbeid utenfor Norge
**Kode:** `JobbetUtenforNorge`

### Beskrivelse
Søker har oppgitt arbeid utenfor Norge i søknaden. <br>Sjekk om disse arbeidsforholdene skal være med i vurderingen av retten til dagpenger.

---
## Manglende vurdering av reell arbeidssøker
**Kode:** `ManglerReellArbeidssøker`

### Beskrivelse
Vurdering av reell arbeidssøker mangler. Utfør vurderingen før innvilgelse.

---
## Gjenopptak i Arena
**Kode:** `MuligGjenopptak`

### Beskrivelse
Søker har åpne saker i Arena som kan være gjenopptak. Sjekk om saken kan gjenopptas i Arena.

---
## Grunnbeløpet for dagpengegrunnlag
**Kode:** `NyttGrunnbeløpForGrunnlag`

### Beskrivelse
Prøvingsdatoen er 1. mai eller senere. Grunnbeløpet for inneværende år var ikke iverksatt på behandlingstidspunktet.<br>Hvis grunnbeløpet ikke er vedtatt ennå, kan behandlingen godkjennes med det gamle grunnbeløpet. Det blir G-justert i Arena.<br>Er grunnbeløpet for inneværende år vedtatt, kjør behandlingen på nytt, og huk av 'Grunnbeløp for grunnlag' for å oppdatere grunnbeløpet.

### Tilknyttet regelsett
- [§ 4-11. Dagpengegrunnlag](./opplysninger.approved.md#-4-11-dagpengegrunnlag)
---
## Prøvingsdato er etter rapporteringsfrist
**Kode:** `PrøvingsdatoEtterRapporteringsfrist`

### Beskrivelse
Prøvingsdatoen er etter neste rapporteringsperiode for inntekt. <br>Vurder om du bør vente til etter A-ordningens rapporteringsfrist for å få med korrekte inntekter.

---
## Unntak til å være reell arbeidssøker
**Kode:** `ReellArbeidssøkerUnntak`

### Beskrivelse
Vurder om søker fyller unntakene til reell arbeidssøker.

### Tilknyttet regelsett
- [§ 4-5. Reelle arbeidssøkere](./opplysninger.approved.md#-4-5-reelle-arbeidssøkere)
---
## Samordning
**Kode:** `Samordning`

### Beskrivelse
Vi har funnet andre ytelser fra folketrygden. <br>Vurder om, og eventuelt hvordan, de skal samordnes med dagpengene.

### Tilknyttet regelsett
- [§ 4-25. Samordning med reduserte ytelser fra folketrygden, eller redusert avtalefestet pensjon](./opplysninger.approved.md#-4-25-samordning-med-reduserte-ytelser-fra-folketrygden,-eller-redusert-avtalefestet-pensjon)
---
## Prøvingsdato
**Kode:** `SjekkPrøvingsdato`

### Beskrivelse
Sjekk at valgt prøvingsdato er riktig.

---
## Svangerskapsrelaterte sykepenger
**Kode:** `SvangerskapsrelaterteSykepenger`

### Beskrivelse
Søker har fått utbetalt sykepenger. Sjekk om sykepengene er svangerskapsrelaterte, <br>og skal være med i inntektsgrunnlaget for vurderingen av minste arbeidsinntekt.

### Tilknyttet regelsett
- [§ 4-4. Krav til minsteinntekt](./opplysninger.approved.md#-4-4-krav-til-minsteinntekt)
---
## Søknadstidspunktet ligger for langt fram i tid
**Kode:** `SøknadstidspunktForLangtFramITid`

### Beskrivelse
Søknadstidspunktet ligger mer enn 14 dager fram i tid.

---
## Søkt gjenopptak
**Kode:** `SøktGjenopptak`

### Beskrivelse
Søker har søkt om gjenopptak. Saker som skal gjenopptas må håndteres i Arena.

---
## Du må velge kun én beregningsregel for tap av arbeidsinntekt og arbeidstid
**Kode:** `TapAvArbeidsinntektOgArbeidstid`

### Beskrivelse
Kun én beregningsregel kan være gyldig til enhver tid. <br>Velg en av "Arbeidstid siste 6 måneder", "Arbeidstid siste 12 måneder" eller "Arbeidstid siste 36 måneder".

### Tilknyttet regelsett
- [§ 4-3. Tap av arbeidsinntekt og arbeidstid](./opplysninger.approved.md#-4-3-tap-av-arbeidsinntekt-og-arbeidstid)
❌ Kan ikke kvitteres

---
## Verneplikt
**Kode:** `Verneplikt`

### Beskrivelse
Søker har svart ja på avtjent verneplikt i søknaden. <br>Vurder om kravet til dagpenger ved avtjent verneplikt er oppfylt.

### Tilknyttet regelsett
- [§ 4-19. Dagpenger etter avtjent verneplikt](./opplysninger.approved.md#-4-19-dagpenger-etter-avtjent-verneplikt)
---
## Prøvingsdato ligger for langt fram i tid
**Kode:** `VirkningstidspunktForLangtFramItid`

### Beskrivelse
Prøvingsdato ligger mer enn 14 dager fram i tid.

### Tilknyttet regelsett
- [§ 3-1. Søknadstidspunkt](./opplysninger.approved.md#-3-1-søknadstidspunkt)
---
## Ytelser utenfor folketrygden
**Kode:** `YtelserUtenforFolketrygden`

### Beskrivelse
Søker har oppgitt i søknaden at hen mottar ytelser utenfor folketrygden.<br>Sjekk hvilke ytelser som er oppgitt utenfor folketrygden og om dette har konsekvenser for dagpengene.

### Tilknyttet regelsett
- [§ 4-26. Samordning med ytelser utenfor folketrygden](./opplysninger.approved.md#-4-26-samordning-med-ytelser-utenfor-folketrygden)
---
