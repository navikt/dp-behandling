package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Avklaringkode

@Suppress("ktlint:standard:max-line-length")
object Avklaringspunkter {
    val YtelserUtenforFolketrygden: Avklaringkode =
        Avklaringkode(
            kode = "YtelserUtenforFolketrygden",
            tittel = "Ytelser utenfor folketrygden",
            beskrivelse =
                """
                Søker har oppgitt i søknaden at hen mottar ytelser utenfor folketrygden.
                Sjekk hvilke ytelser som er oppgitt utenfor folketrygden og om dette har konsekvenser for dagpengene.
                """.trimIndent(),
        )

    val Samordning =
        Avklaringkode(
            kode = "Samordning",
            tittel = "Samordning",
            beskrivelse =
                """
                Vi har funnet andre ytelser fra folketrygden. 
                Vurder om, og eventuelt hvordan, de skal samordnes med dagpengene.
                """.trimIndent(),
        )

    val FulleYtelser: Avklaringkode =
        Avklaringkode(
            kode = "FulleYtelser",
            tittel = "Andre fulle ytelser etter folketrygdloven",
            beskrivelse =
                """
                Sjekk om søker har andre fulle ytelser. 
                Om søker har andre fulle ytelser, må det velges mellom dagpenger eller disse ytelsene.
                """.trimIndent(),
        )

    val SøknadstidspunktForLangtFramITid =
        Avklaringkode(
            kode = "SøknadstidspunktForLangtFramITid",
            tittel = "Søknadstidspunktet ligger for langt fram i tid",
            beskrivelse = "Søknadstidspunktet ligger mer enn 14 dager fram i tid.",
        )

    val VirkningstidspunktForLangtFramITid =
        Avklaringkode(
            kode = "VirkningstidspunktForLangtFramItid",
            tittel = "Prøvingsdato ligger for langt fram i tid",
            beskrivelse = "Prøvingsdato ligger mer enn 14 dager fram i tid.",
        )

    val SjekkPrøvingsdato =
        Avklaringkode(
            kode = "SjekkPrøvingsdato",
            tittel = "Prøvingsdato",
            beskrivelse = "Sjekk at valgt prøvingsdato er riktig.",
        )

    val Verneplikt =
        Avklaringkode(
            kode = "Verneplikt",
            tittel = "Verneplikt",
            beskrivelse =
                """
                Søker har svart ja på avtjent verneplikt i søknaden. 
                Vurder om kravet til dagpenger ved avtjent verneplikt er oppfylt.
                """.trimIndent(),
        )

    val TapAvArbeidstidBeregningsregel =
        Avklaringkode(
            kode = "TapAvArbeidsinntektOgArbeidstid",
            tittel = "Du må velge kun én beregningsregel for tap av arbeidsinntekt og arbeidstid",
            kanKvitteres = false,
            beskrivelse =
                """
                Kun én beregningsregel kan være gyldig til enhver tid. 
                Velg en av "Arbeidstid siste 6 måneder", "Arbeidstid siste 12 måneder" eller "Arbeidstid siste 36 måneder".
                """.trimIndent(),
        )

    val BeregnetArbeidstid =
        Avklaringkode(
            kode = "BeregnetArbeidstid",
            tittel = "Arbeidstid",
            beskrivelse =
                """
                Sjekk om vanlig arbeidstid er korrekt og at det er brukt riktig beregningsregel. 
                Du må også sjekke om ny vanlig arbeidstid er korrekt.
                """.trimIndent(),
        )

    val EØSArbeid =
        Avklaringkode(
            kode = "EØSArbeid",
            tittel = "Arbeid i EØS, Sveits eller Storbritannia",
            beskrivelse =
                """
                Søker har oppgitt arbeid fra EØS, Sveits eller Storbritannia i søknaden. 
                Vurder om det skal være sammenlegging.
                """.trimIndent(),
        )

    val JobbetOverTerskel =
        Avklaringkode(
            kode = "JobbetOverTerskel",
            tittel = "Stans av dagpenger på grunn av arbeid",
            beskrivelse =
                """
                På de siste tre meldekortene har bruker ført jobb som utgjør mer enn 50 prosent av sin vanlige arbeidstid. Vilkåret om tre påfølgende meldeperioder uten tilstrekkelig tap av arbeidstid etter dagpengeforskriften § 10-4 er ikke oppfylt og dagpenger skal stanses fra og med mandag etter siste innsendte meldekort.
                """.trimIndent(),
            kanAvbrytes = false,
        )

    val JobbetUtenforNorge =
        Avklaringkode(
            kode = "JobbetUtenforNorge",
            tittel = "Arbeid utenfor Norge",
            beskrivelse =
                """
                Søker har oppgitt arbeid utenfor Norge i søknaden. 
                Sjekk om disse arbeidsforholdene skal være med i vurderingen av retten til dagpenger.
                """.trimIndent(),
        )

    val HattLukkedeSakerSiste8Uker =
        Avklaringkode(
            kode = "HattLukkedeSakerSiste8Uker",
            tittel = "Nylig lukkede saker i Arena",
            beskrivelse =
                """
                Søker har lukkede saker i Arena fra de siste 8 ukene. Sjekk om disse kan påvirke behandlingen.
                Hvis vi nylig har gitt avslag, sjekk om det er nødvendig med ekstra veiledning.
                """.trimIndent(),
        )

    val SøktGjenopptak =
        Avklaringkode(
            kode = "SøktGjenopptak",
            tittel = "Søkt gjenopptak, men har ikke noe historikk å ta utgangspunkt i.",
            beskrivelse = "Søker har søkt om gjenopptak.",
            kanAvbrytes = false,
        )

    val MuligGjenopptak =
        Avklaringkode(
            kode = "MuligGjenopptak",
            tittel = "Gjenopptak i Arena",
            beskrivelse = "Søker har åpne saker i Arena som kan være gjenopptak. Sjekk om saken kan gjenopptas i Arena.",
        )

    val GjenopptakBehandling =
        Avklaringkode(
            kode = "GjenopptakBehandling",
            tittel = "Behandles som gjenopptak i ny løsning",
            beskrivelse = "Denne saken har en innvilget behandling og det må vurderes om retten skal gjenopptas.",
            kanAvbrytes = false,
        )

    val InntektNesteKalendermåned =
        Avklaringkode(
            kode = "InntektNesteKalendermåned",
            tittel = "Inntekt neste måned",
            beskrivelse =
                """
                Søker har inntekt som tilhører neste kalendermåned. 
                Sjekk om det er tilstrekkelige inntekter til at utfallet eller dagpengegrunnlaget vil endre seg i neste kalendermåned.
                """.trimIndent(),
        )

    val InntektManueltRedigert =
        Avklaringkode(
            kode = "InntektManueltRedigert",
            tittel = "Manuelt redigert inntekt",
            beskrivelse =
                """
                Inntekten er manuelt redigert. Du må begrunne årsaken.
                """.trimIndent(),
        )

    val Inntektsjekk =
        Avklaringkode(
            kode = "Inntektsjekk",
            tittel = "Minsteinntekt - inntekter må kontrolleres",
            beskrivelse =
                """
                Registeropplysninger om inntekter indikerer at brukeren ikke oppfyller kravet til minste arbeidsinntekt. 
                Som følge av vansker med å rapportere i A-inntekt etter den 15.juni 2026, kan det være at vi ikke har alle nødvendige registeropplysninger. 
                Kontroller A-inntekt før saken behandles ferdig.
                """.trimIndent(),
        )

    val SvangerskapsrelaterteSykepenger =
        Avklaringkode(
            kode = "SvangerskapsrelaterteSykepenger",
            tittel = "Svangerskapsrelaterte sykepenger",
            beskrivelse =
                """
                Søker har fått utbetalt sykepenger. Sjekk om sykepengene er svangerskapsrelaterte, 
                og skal være med i inntektsgrunnlaget for vurderingen av minste arbeidsinntekt.
                """.trimIndent(),
        )

    val PrøvingsdatoEtterRapporteringsfrist =
        Avklaringkode(
            kode = "PrøvingsdatoEtterRapporteringsfrist",
            tittel = "Prøvingsdato er etter rapporteringsfrist",
            beskrivelse =
                """
                Prøvingsdatoen er etter neste rapporteringsperiode for inntekt. 
                Vurder om du bør vente til etter A-ordningens rapporteringsfrist for å få med korrekte inntekter.
                """.trimIndent(),
        )

    val BrukerUnder18 =
        Avklaringkode(
            kode = "BrukerUnder18",
            tittel = "Bruker er under 18 år",
            beskrivelse = "Søker er under 18 år og skal ikke ha automatisk behandling.",
        )

    val BarnMåGodkjennes =
        Avklaringkode(
            kode = "BarnMåGodkjennes",
            tittel = "Barnetillegg",
            beskrivelse = "Barn må godkjennes om de skal gi barnetillegg. Sjekk hvilke barn som skal gi barnetillegg.",
        )

    val ReellArbeidssøkerUnntak =
        Avklaringkode(
            kode = "ReellArbeidssøkerUnntak",
            tittel = "Unntak til å være reell arbeidssøker",
            beskrivelse = "Vurder om søker fyller unntakene til reell arbeidssøker.",
        )

    val IkkeRegistrertSomArbeidsøker =
        Avklaringkode(
            kode = "IkkeRegistrertSomArbeidsøker",
            tittel = "Ikke registrert som arbeidssøker",
            beskrivelse = "Søker er ikke registrert som arbeidssøker på søknadstidspunktet.",
        )

    val IkkeOppfyllerMeldeplikt =
        Avklaringkode(
            kode = "IkkeOppfyllerMeldeplikt",
            tittel = "Ikke oppfylt meldeplikt",
            beskrivelse = "Bruker har ikke meldt seg i tide (§4-8). Vurder om dagpenger skal stanses.",
        )

    val HarOppgittPermittering =
        Avklaringkode(
            kode = "HarOppgittPermittering",
            tittel = "Permittering",
            beskrivelse = "Søker har oppgitt permittering i søknaden. Vurder om søker er permittert og har rett til dagpenger som permittert.",
        )

    val HarSvartPåOmgjøringUtenKlage =
        Avklaringkode(
            kode = "HarSvartPåOmgjøringUtenKlage",
            tittel = "Omgjøring etter forvaltningsloven § 35",
            beskrivelse = "Du må velge aktuell bokstav i forvaltningsloven § 35, og begrunne. Trykk på linken for å komme til prosessregler og gi begrunnelse.",
            kanKvitteres = false,
        )

    val SkalOmgjøringUtenKlageVurderes =
        Avklaringkode(
            kode = "SkalOmgjøringUtenKlageVurderes",
            tittel = "Skal vedtaket omgjøres etter forvaltningsloven § 35?",
            beskrivelse =
                """
                Hvis du ikke skal omgjøre etter forvaltningsloven § 35, må du begrunne hvorfor for å lukke avklaringen. 
                Hvis du skal omgjøre må du klikke på linken og velge årsak til omgjøringen.
                """.trimIndent(),
            kanKvitteres = true,
        )

    val HarTilleggsopplysninger =
        Avklaringkode(
            kode = "HarTilleggsopplysninger",
            tittel = "Tilleggsopplysninger",
            beskrivelse =
                """
                Søker har oppgitt tilleggsopplysninger i søknaden. Vurder om tilleggsopplysninger har betydning for saken.
                I tillegg bør det vurderes om opplysningene skal videreformidles til Nav lokal eller om søker trenger veiledning.
                """.trimIndent(),
        )

    val HarOppgittPermitteringFiskeindustri =
        Avklaringkode(
            kode = "HarOppgittPermitteringFiskeindustri",
            tittel = "Permittering fiskeindustri",
            beskrivelse =
                """
                Søker har oppgitt permittering fra fiskeindustri i søknaden. 
                Vurder om søker er permittert og har rett til dagpenger som permittert fra fiskeindustrien.
                """.trimIndent(),
        )

    val GrunnbeløpForGrunnlagEndret =
        Avklaringkode(
            kode = "NyttGrunnbeløpForGrunnlag",
            tittel = "Grunnbeløpet for dagpengegrunnlag",
            beskrivelse =
                """
                Prøvingsdatoen er 1. mai eller senere. Grunnbeløpet for inneværende år var ikke iverksatt på behandlingstidspunktet.
                Hvis grunnbeløpet ikke er vedtatt ennå, kan behandlingen godkjennes med det gamle grunnbeløpet. Det blir G-justert i Arena.
                Er grunnbeløpet for inneværende år vedtatt, kjør behandlingen på nytt, og huk av 'Grunnbeløp for grunnlag' for å oppdatere grunnbeløpet.
                """.trimIndent(),
        )

    val Bostedsland =
        Avklaringkode(
            kode = "Bostedsland",
            tittel = "Bostedsland er ikke Norge",
            beskrivelse =
                """
                Søker har oppgitt bostedsland som ikke er i Norge.
                Sjekk om bruker er unntatt vilkårene for opphold i Norge.
                """.trimIndent(),
        )

    val ManglerReellArbeidssøker =
        Avklaringkode(
            kode = "ManglerReellArbeidssøker",
            tittel = "Manglende vurdering av reell arbeidssøker",
            beskrivelse =
                """
                Vurdering av reell arbeidssøker mangler. Utfør vurderingen før innvilgelse.
                """.trimIndent(),
        )

    val StansAlder =
        Avklaringkode(
            kode = "StansAlder",
            tittel = "Bortfall på grunn av alder",
            beskrivelse =
                """
                Retten til dagpenger faller bort ved utgangen av den måneden bruker fyller 67 år, vurder stans av dagpenger. 
                """.trimIndent(),
        )

    val Utestengt =
        Avklaringkode(
            kode = "Utestengt",
            tittel = "Bruker er utestengt",
            beskrivelse = "Bruker er utestengt. Sjekk at dette stemmer.",
        )

    val OppgirSelvforskyldtArbeidsløshet =
        Avklaringkode(
            kode = "OppgirSelvforskyldtArbeidsløshet",
            tittel = "Bruker oppgir selvforskyldt arbeidsløshet",
            beskrivelse = "Bruker oppgir selvforskyldt arbeidsløshet i søknaden. Sjekk at dette stemmer.",
        )
}
