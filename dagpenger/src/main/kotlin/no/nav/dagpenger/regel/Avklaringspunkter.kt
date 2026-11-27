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

    val SøknadBehandletTidligere =
        Avklaringkode(
            kode = "SøknadBehandletTidligere",
            tittel = "Det finnes en tidligere behandling av samme søknad",
            beskrivelse =
                """
                Denne søknaden er allerede behandlet. Vurder om det skal gjøres endringer fra sist, eller avbryt behandlingen.
                """.trimIndent(),
            kanAvbrytes = false,
        )

    val meldekortMedUtdanning =
        Avklaringkode(
            kode = "MeldekortMedUtdanning",
            tittel = "Meldekort med utdanning eller tiltak",
            beskrivelse =
                """Bruker har krysset av for utdanning eller tiltak på meldekortet. Må vurderes manuelt. 
                |Husk å sjekke om det er godkjent arbeidsmarkedstiltak i Arena.
                """.trimMargin(),
            kanKvitteres = true,
        )

    val GjenopptakEtter12Uker =
        Avklaringkode(
            kode = "GjenopptakEtter12Uker",
            tittel = "Avbruddsperioden er 12 uker eller mer",
            beskrivelse = "Avbruddsperioden er beregnet til 12 uker eller mer. Systemet har ikke kontrollert om det er arbeid i perioden eller ikke. Du må sjekke om bruker har arbeidet, og i så fall hvor mye, for å kontrollere om vilkåret til reberegning ved gjenopptak er oppfylt.",
        )

    val OmgjøringEtterKlage =
        Avklaringkode(
            kode = "OmgjøringEtterKlage",
            tittel = "Omgjøring etter klage",
            beskrivelse = "Behandlingen er en omgjøring etter klage og kan ikke automatisk behandles",
            kanAvbrytes = false,
        )

    val KorrigeringUtbetaltPeriode =
        Avklaringkode(
            kode = "KorrigeringUtbetaltPeriode",
            tittel = "Beregning av meldekort som korrigerer tidligere periode",
            beskrivelse = "Behandlingen er korrigering av et tidligere beregnet meldekort",
            kanAvbrytes = false,
        )

    val EtterrapportertPeriode =
        Avklaringkode(
            kode = "EtterrapportertPeriode",
            tittel = "Beregning av meldekort som gjelder en periode som ligger bak i tid",
            beskrivelse = "Behandlingen gjelder et meldekort som er rapportert etter at vi har beregnet en senere periode",
            kanAvbrytes = false,
        )

    val MeldekortMedEksport =
        Avklaringkode(
            kode = "MeldekortMedEksport",
            tittel = "Meldekort med eksport",
            beskrivelse =
                """Bruker er innvilget eksport. Sjekk at det beregnes riktig, 
                |og ikke får f.eks. trekk ved for sen melding.
                """.trimMargin(),
            kanAvbrytes = false,
            kanKvitteres = true,
        )

    val MigrertYtelse =
        Avklaringkode(
            kode = "MigrertYtelse",
            tittel = "Migrert ytelse",
            beskrivelse = "Ytelsen er migrert fra et annet system og må kontrolleres",
            kanAvbrytes = false,
        )

    val ManueltUtmeldt =
        Avklaringkode(
            kode = "ManueltUtmeldt",
            tittel = "Bruker har blitt utmeldt av ASR utenfor dagpenger",
            beskrivelse =
                """Bruker har blitt utmeldt av ASR utenfor dagpenger, og må derfor vurderes manuelt. Sjekk 
                |hvordan dette påvirker retten til dagpenger og fra hvilken dato en eventuell stans skal gjelde fra.
                """.trimMargin(),
            kanAvbrytes = false,
        )

    val UtmeldtArbeidssøker =
        Avklaringkode(
            kode = "UtmeldtArbeidssøker",
            tittel = "Bruker har blitt utmeldt av arbeidssøkerregisteret",
            beskrivelse = "Bruker har blitt utmeldt av arbeidssøkerregisteret. Ta stilling til om forslaget til stans er riktig.",
            kanAvbrytes = false,
        )

    val MuligSamordning =
        Avklaringkode(
            kode = "MuligSamordning",
            tittel = "Mulig samordning mot annen ytelse",
            beskrivelse = "Behandlingen er opprettet manuelt og kan ikke automatisk behandles",
            kanAvbrytes = false,
        )

    val ManuellBehandling =
        Avklaringkode(
            kode = "ManuellBehandling",
            tittel = "Manuell behandling",
            beskrivelse = "Behandlingen er opprettet manuelt og kan ikke automatisk behandles",
            kanAvbrytes = false,
        )

    // copy() gir samme kode, så avklaringen regnes som lik og registreres ikke på nytt
    fun utmeldtArbeidssøker(beskrivelse: String) =
        UtmeldtArbeidssøker.copy(beskrivelse = "$beskrivelse. Ta stilling til om forslaget til stans er riktig.")

    fun muligSamordning(begrunnelse: String?) = MuligSamordning.copy(beskrivelse = begrunnelse ?: MuligSamordning.beskrivelse)

    fun manuellBehandling(begrunnelse: String?) = ManuellBehandling.copy(beskrivelse = begrunnelse ?: ManuellBehandling.beskrivelse)
}
