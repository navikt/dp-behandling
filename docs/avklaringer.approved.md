# Avklaringer
Avklaringer opprettes hvor regelmotoren er usikker på enten fakta eller riktig vei videre.

Avklaringer opprettes av "kontrollpunkt" som gjør en vurdering av opplysninger og ser om avklaringen er nødvendig.

Endringer i opplysninger vil automatisk lukke avklaringen om kontrollpunktet sier den ikke lenger er nødvendig.
Tilsvarende vil avklaringen åpnes opp igjen om opplysningene endres.

## Forklaring av egenskaper

**Kan lukkes av saksbehandler** betyr at saksbehandler selv kan markere avklaringen som håndtert uten å endre fakta i behandlingen.
Avklaringer som *ikke* kan lukkes av saksbehandler krever at opplysningene i saken faktisk endres – ellers lukkes de ikke.

**Lukkes automatisk** betyr at systemet kan lukke avklaringen automatisk når de underliggende opplysningene endres og behovet for avklaringern forsvinner.

Avklaringer som *ikke* kan lukkes automatisk må alltid håndteres manuelt av saksbehandler, uavhengig av opplysningene.

## Barnetillegg
- **Kode:** `BarnMåGodkjennes`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Barn må godkjennes om de skal gi barnetillegg. Sjekk hvilke barn som skal gi barnetillegg.

### Tilknyttet regelsett
- [§ 4-12. Dagpengenes størrelse](./opplysninger.approved.md#-4-12-dagpengenes-størrelse)
### Opplysninger avklaringen ser på
- Barn

---
## Arbeidstid
- **Kode:** `BeregnetArbeidstid`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Sjekk om vanlig arbeidstid er korrekt og at det er brukt riktig beregningsregel. <br>Du må også sjekke om ny vanlig arbeidstid er korrekt.

### Tilknyttet regelsett
- [§ 4-3. Tap av arbeidsinntekt og arbeidstid](./opplysninger.approved.md#-4-3-tap-av-arbeidsinntekt-og-arbeidstid)
### Opplysninger avklaringen ser på
- Beregnet vanlig arbeidstid per uke før tap

---
## Bostedsland er ikke Norge
- **Kode:** `Bostedsland`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har oppgitt bostedsland som ikke er i Norge.<br>Sjekk om bruker er unntatt vilkårene for opphold i Norge.

### Tilknyttet regelsett
- [§ 4-2. Opphold i Norge](./opplysninger.approved.md#-4-2-opphold-i-norge)
### Opplysninger avklaringen ser på
- Oppfyller unntak for opphold i Norge

---
## Bruker er under 18 år
- **Kode:** `BrukerUnder18`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker er under 18 år og skal ikke ha automatisk behandling.

### Opplysninger avklaringen ser på
- Fødselsdato

---
## Arbeid i EØS, Sveits eller Storbritannia
- **Kode:** `EØSArbeid`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har oppgitt arbeid fra EØS, Sveits eller Storbritannia i søknaden. <br>Vurder om det skal være sammenlegging.

### Opplysninger avklaringen ser på
- Inntektsopplysninger

---
## Andre fulle ytelser etter folketrygdloven
- **Kode:** `FulleYtelser`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Sjekk om søker har andre fulle ytelser. <br>Om søker har andre fulle ytelser, må det velges mellom dagpenger eller disse ytelsene.

### Tilknyttet regelsett
- [§ 4-24. Medlem som har fulle ytelser etter folketrygdloven eller avtalefestet pensjon](./opplysninger.approved.md#-4-24-medlem-som-har-fulle-ytelser-etter-folketrygdloven-eller-avtalefestet-pensjon)
### Opplysninger avklaringen ser på
- Medlem har reduserte ytelser fra folketrygden (Samordning)

---
## Behandles som gjenopptak i ny løsning
- **Kode:** `GjenopptakBehandling`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ❌ 
### Beskrivelse
Denne saken har en innvilget behandling og det må vurderes om retten skal gjenopptas.

---
## Permittering
- **Kode:** `HarOppgittPermittering`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har oppgitt permittering i søknaden. Vurder om søker er permittert og har rett til dagpenger som permittert.

### Tilknyttet regelsett
- [§ 4-7. Dagpenger til permitterte](./opplysninger.approved.md#-4-7-dagpenger-til-permitterte)
### Opplysninger avklaringen ser på
- Årsaken til permitteringen er godkjent

---
## Permittering fiskeindustri
- **Kode:** `HarOppgittPermitteringFiskeindustri`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har oppgitt permittering fra fiskeindustri i søknaden. <br>Vurder om søker er permittert og har rett til dagpenger som permittert fra fiskeindustrien.

### Tilknyttet regelsett
- [§ 6-7. Permittering i fiskeforedlingsindustrien, sjømatindustrien og fiskeoljeindustrien](./opplysninger.approved.md#-6-7-permittering-i-fiskeforedlingsindustrien,-sjømatindustrien-og-fiskeoljeindustrien)
### Opplysninger avklaringen ser på
- Permittert fra fiskeindustrien

---
## Omgjøring uten klage
- **Kode:** `HarSvartPåOmgjøringUtenKlage`
- **Kan lukkes av saksbehandler:** ❌ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Saksbehandler må svare på hvorfor vedtaket omgjøres uten at det har vært klage på tidligere vedtak. Sjekk at det er lagt inn en begrunnelse for omgjøringen.

### Tilknyttet regelsett
- [§ 6-35. Omgjøring av vedtak uten klage](./opplysninger.approved.md#-6-35-omgjøring-av-vedtak-uten-klage)
### Opplysninger avklaringen ser på
- Et forvaltningsorgan kan omgjøre sitt eget vedtak uten at det er påklaget

---
## Tilleggsopplysninger
- **Kode:** `HarTilleggsopplysninger`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har oppgitt tilleggsopplysninger i søknaden. Vurder om tilleggsopplysninger har betydning for saken.<br>I tillegg bør det vurderes om opplysningene skal videreformidles til Nav lokal eller om søker trenger veiledning.

### Opplysninger avklaringen ser på
- søknadId

---
## Nylig lukkede saker i Arena
- **Kode:** `HattLukkedeSakerSiste8Uker`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har lukkede saker i Arena fra de siste 8 ukene. Sjekk om disse kan påvirke behandlingen.<br>Hvis vi nylig har gitt avslag, sjekk om det er nødvendig med ekstra veiledning.

### Opplysninger avklaringen ser på
- Oppfyller kravet til alder

---
## Ikke oppfylt meldeplikt
- **Kode:** `IkkeOppfyllerMeldeplikt`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Bruker har ikke meldt seg i tide (§4-8). Vurder om dagpenger skal stanses.

---
## Ikke registrert som arbeidssøker
- **Kode:** `IkkeRegistrertSomArbeidsøker`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker er ikke registrert som arbeidssøker på søknadstidspunktet.

### Tilknyttet regelsett
- [§ 4-5. Reelle arbeidssøkere](./opplysninger.approved.md#-4-5-reelle-arbeidssøkere)
### Opplysninger avklaringen ser på
- Oppfyller kravet til å være registrert som arbeidssøker

---
## Manuelt redigert inntekt
- **Kode:** `InntektManueltRedigert`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Inntekten er manuelt redigert. Du må begrunne årsaken.

### Opplysninger avklaringen ser på
- Inntektsopplysninger

---
## Inntekt neste måned
- **Kode:** `InntektNesteKalendermåned`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har inntekt som tilhører neste kalendermåned. <br>Sjekk om det er tilstrekkelige inntekter til at utfallet eller dagpengegrunnlaget vil endre seg i neste kalendermåned.

### Tilknyttet regelsett
- [§ 4-4. Krav til minsteinntekt](./opplysninger.approved.md#-4-4-krav-til-minsteinntekt)
### Opplysninger avklaringen ser på
- Inntektsopplysninger

---
## Minsteinntekt - inntekter må kontrolleres
- **Kode:** `Inntektsjekk`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Registeropplysninger om inntekter indikerer at brukeren ikke oppfyller kravet til minste arbeidsinntekt. <br>Som følge av vansker med å rapportere i A-inntekt etter den 15.juni 2026, kan det være at vi ikke har alle nødvendige registeropplysninger. <br>Kontroller A-inntekt før saken behandles ferdig.

### Opplysninger avklaringen ser på
- Inntektsopplysninger

---
## Stans av dagpenger på grunn av arbeid
- **Kode:** `JobbetOverTerskel`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ❌ 
### Beskrivelse
På de siste tre meldekortene har bruker ført jobb som utgjør mer enn 50 prosent av sin vanlige arbeidstid. Vilkåret om tre påfølgende meldeperioder uten tilstrekkelig tap av arbeidstid etter dagpengeforskriften § 10-4 er ikke oppfylt og dagpenger skal stanses fra og med mandag etter siste innsendte meldekort.

### Tilknyttet regelsett
- [§ 10-4. Tre påfølgende meldeperioder uten tilstrekkelig tap av arbeidstid](./opplysninger.approved.md#-10-4-tre-påfølgende-meldeperioder-uten-tilstrekkelig-tap-av-arbeidstid)
---
## Arbeid utenfor Norge
- **Kode:** `JobbetUtenforNorge`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har oppgitt arbeid utenfor Norge i søknaden. <br>Sjekk om disse arbeidsforholdene skal være med i vurderingen av retten til dagpenger.

### Opplysninger avklaringen ser på
- Inntektsopplysninger

---
## Manglende vurdering av reell arbeidssøker
- **Kode:** `ManglerReellArbeidssøker`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Vurdering av reell arbeidssøker mangler. Utfør vurderingen før innvilgelse.

### Opplysninger avklaringen ser på
- Oppfyller kravet til minsteinntekt

---
## Gjenopptak i Arena
- **Kode:** `MuligGjenopptak`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har åpne saker i Arena som kan være gjenopptak. Sjekk om saken kan gjenopptas i Arena.

### Opplysninger avklaringen ser på
- Oppfyller kravet til alder

---
## Grunnbeløpet for dagpengegrunnlag
- **Kode:** `NyttGrunnbeløpForGrunnlag`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Prøvingsdatoen er 1. mai eller senere. Grunnbeløpet for inneværende år var ikke iverksatt på behandlingstidspunktet.<br>Hvis grunnbeløpet ikke er vedtatt ennå, kan behandlingen godkjennes med det gamle grunnbeløpet. Det blir G-justert i Arena.<br>Er grunnbeløpet for inneværende år vedtatt, kjør behandlingen på nytt, og huk av 'Grunnbeløp for grunnlag' for å oppdatere grunnbeløpet.

### Tilknyttet regelsett
- [§ 4-11. Dagpengegrunnlag](./opplysninger.approved.md#-4-11-dagpengegrunnlag)
### Opplysninger avklaringen ser på
- Grunnbeløp for grunnlag

---
## Bruker oppgir selvforskyldt arbeidsløshet
- **Kode:** `OppgirSelvforskyldtArbeidsløshet`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Bruker oppgir selvforskyldt arbeidsløshet i søknaden. Sjekk at dette stemmer.

### Tilknyttet regelsett
- [§ 4-10. Sanksjonsperiode ved selvforskyldt arbeidsløshet](./opplysninger.approved.md#-4-10-sanksjonsperiode-ved-selvforskyldt-arbeidsløshet)
### Opplysninger avklaringen ser på
- Bruker oppgir selvforskyldt arbeidsløshet

---
## Prøvingsdato er etter rapporteringsfrist
- **Kode:** `PrøvingsdatoEtterRapporteringsfrist`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Prøvingsdatoen er etter neste rapporteringsperiode for inntekt. <br>Vurder om du bør vente til etter A-ordningens rapporteringsfrist for å få med korrekte inntekter.

### Opplysninger avklaringen ser på
- Arbeidsgivers rapporteringsfrist

---
## Unntak til å være reell arbeidssøker
- **Kode:** `ReellArbeidssøkerUnntak`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Vurder om søker fyller unntakene til reell arbeidssøker.

### Tilknyttet regelsett
- [§ 4-5. Reelle arbeidssøkere](./opplysninger.approved.md#-4-5-reelle-arbeidssøkere)
### Opplysninger avklaringen ser på
- Kravet til reell arbeidssøker er relevant

---
## Samordning
- **Kode:** `Samordning`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Vi har funnet andre ytelser fra folketrygden. <br>Vurder om, og eventuelt hvordan, de skal samordnes med dagpengene.

### Tilknyttet regelsett
- [§ 4-25. Samordning med reduserte ytelser fra folketrygden, eller redusert avtalefestet pensjon](./opplysninger.approved.md#-4-25-samordning-med-reduserte-ytelser-fra-folketrygden,-eller-redusert-avtalefestet-pensjon)
### Opplysninger avklaringen ser på
- Medlem har reduserte ytelser fra folketrygden (Samordning)

---
## Prøvingsdato
- **Kode:** `SjekkPrøvingsdato`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Sjekk at valgt prøvingsdato er riktig.

### Opplysninger avklaringen ser på
- Prøvingsdato

---
## Bortfall på grunn av alder
- **Kode:** `StansAlder`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Retten til dagpenger faller bort ved utgangen av den måneden bruker fyller 67 år, vurder stans av dagpenger. 

---
## Svangerskapsrelaterte sykepenger
- **Kode:** `SvangerskapsrelaterteSykepenger`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har fått utbetalt sykepenger. Sjekk om sykepengene er svangerskapsrelaterte, <br>og skal være med i inntektsgrunnlaget for vurderingen av minste arbeidsinntekt.

### Tilknyttet regelsett
- [§ 4-4. Krav til minsteinntekt](./opplysninger.approved.md#-4-4-krav-til-minsteinntekt)
### Opplysninger avklaringen ser på
- Inntektsopplysninger

---
## Søknadstidspunktet ligger for langt fram i tid
- **Kode:** `SøknadstidspunktForLangtFramITid`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søknadstidspunktet ligger mer enn 14 dager fram i tid.

---
## Søkt gjenopptak, men har ikke noe historikk å ta utgangspunkt i.
- **Kode:** `SøktGjenopptak`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ❌ 
### Beskrivelse
Søker har søkt om gjenopptak.

---
## Du må velge kun én beregningsregel for tap av arbeidsinntekt og arbeidstid
- **Kode:** `TapAvArbeidsinntektOgArbeidstid`
- **Kan lukkes av saksbehandler:** ❌ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Kun én beregningsregel kan være gyldig til enhver tid. <br>Velg en av "Arbeidstid siste 6 måneder", "Arbeidstid siste 12 måneder" eller "Arbeidstid siste 36 måneder".

### Tilknyttet regelsett
- [§ 4-3. Tap av arbeidsinntekt og arbeidstid](./opplysninger.approved.md#-4-3-tap-av-arbeidsinntekt-og-arbeidstid)
### Opplysninger avklaringen ser på
- Beregnet vanlig arbeidstid per uke før tap

---
## Bruker er utestengt
- **Kode:** `Utestengt`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Bruker er utestengt. Sjekk at dette stemmer.

### Tilknyttet regelsett
- [§ 4-28. Utestengning](./opplysninger.approved.md#-4-28-utestengning)
### Opplysninger avklaringen ser på
- Bruker er utestengt fra dagpenger

---
## Verneplikt
- **Kode:** `Verneplikt`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har svart ja på avtjent verneplikt i søknaden. <br>Vurder om kravet til dagpenger ved avtjent verneplikt er oppfylt.

### Tilknyttet regelsett
- [§ 4-19. Dagpenger etter avtjent verneplikt](./opplysninger.approved.md#-4-19-dagpenger-etter-avtjent-verneplikt)
### Opplysninger avklaringen ser på
- Avtjent verneplikt

---
## Prøvingsdato ligger for langt fram i tid
- **Kode:** `VirkningstidspunktForLangtFramItid`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Prøvingsdato ligger mer enn 14 dager fram i tid.

### Tilknyttet regelsett
- [§ 3-1. Søknadstidspunkt](./opplysninger.approved.md#-3-1-søknadstidspunkt)
### Opplysninger avklaringen ser på
- Prøvingsdato

---
## Ytelser utenfor folketrygden
- **Kode:** `YtelserUtenforFolketrygden`
- **Kan lukkes av saksbehandler:** ✅ 
- **Lukkes automatisk når opplysningene endres:** ✅ 
### Beskrivelse
Søker har oppgitt i søknaden at hen mottar ytelser utenfor folketrygden.<br>Sjekk hvilke ytelser som er oppgitt utenfor folketrygden og om dette har konsekvenser for dagpengene.

### Tilknyttet regelsett
- [§ 4-26. Samordning med ytelser utenfor folketrygden](./opplysninger.approved.md#-4-26-samordning-med-ytelser-utenfor-folketrygden)
### Opplysninger avklaringen ser på
- Skal samordnes med ytelser utenfor folketrygden

---
