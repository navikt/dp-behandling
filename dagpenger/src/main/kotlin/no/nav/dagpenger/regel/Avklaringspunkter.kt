package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Avklaringkode

@Suppress("ktlint:standard:max-line-length")
object Avklaringspunkter {
    val YtelserUtenforFolketrygden: Avklaringkode =
        Avklaringkode(
            kode = "YtelserUtenforFolketrygden",
            tittel = "Sjekk om det er ytelser utenfor folketrygden som skal samordnes",
            beskrivelse = "Sjekk hvilke ytelser som er oppgitt utenfor folketrygden og om de skal ha konsekvens for dagpengene",
        )

    val Samordning =
        Avklaringkode(
            kode = "Samordning",
            tittel = "Sjekk om det er andre ytelser fra folketrygden som skal samordnes",
            beskrivelse = "Vi har funnet andre ytelser fra folketrygden. Det må vurderes om, og eventuelt hvordan, de skal samordnes med dagpengene.",
        )

    val FulleYtelser: Avklaringkode =
        Avklaringkode(
            kode = "FulleYtelser",
            tittel = "Sjekk om søker har andre fulle ytelser",
            beskrivelse = "Om søker har andre fulle ytelser må det velges mellom dagpenger eller disse ytelsene",
        )

    val SøknadstidspunktForLangtFramITid =
        Avklaringkode(
            kode = "SøknadstidspunktForLangtFramITid",
            tittel = "Søknadstidspunktet ligger for langt fram i tid",
            beskrivelse = "Søknadstidspunktet ligger mer enn 14 dager fram i tid",
        )

    val VirkningstidspunktForLangtFramITid =
        Avklaringkode(
            kode = "VirkningstidspunktForLangtFramItid",
            tittel = "Virkningstidspunkt ligger for langt fram i tid",
            beskrivelse = "Virkningstidspunkt ligger mer enn 14 dager fram i tid",
        )

    val SjekkPrøvingsdato =
        Avklaringkode(
            kode = "SjekkPrøvingsdato",
            tittel = "Sjekk om valgt prøvingsdato er riktig",
            beskrivelse = "Sjekk at valgt prøvingsdato er riktig",
        )

    val Verneplikt =
        Avklaringkode(
            kode = "Verneplikt",
            tittel = "Sjekk om søker oppfyller vilkåret til dagpenger ved avtjent verneplikt",
            beskrivelse =
                """
                Søker har oppgitt at de har avtjent verneplikt. Det må sjekkes om kravet til dagpenger ved avtjent verneplikt er oppfylt.
                """.trimIndent(),
        )

    val TapAvArbeidstidBeregningsregel =
        Avklaringkode(
            kode = "TapAvArbeidsinntektOgArbeidstid",
            tittel = "Velg kun en beregningsregel for tap av arbeidsinntekt og arbeidstid",
            kanKvitteres = false,
            beskrivelse =
                """
                Kun én beregningsregel kan være gyldig til en hver tid. 
                Velg en av Arbeidstid siste 6 måneder, Arbeidstid siste 12 måneder eller Arbeidstid siste 36 måneder.
                """.trimIndent(),
        )

    val BeregnetArbeidstid =
        Avklaringkode(
            kode = "BeregnetArbeidstid",
            tittel = "Sjekk om beregnet arbeidstid er korrekt",
            beskrivelse =
                """
                Du må sjekke om beregnet vanlig arbeidstid er korrekt og at det er brukt riktig beregningsregel. 
                Du må også sjekke om ny vanlig arbeidstid er korrekt
                """.trimIndent(),
        )

    val EØSArbeid =
        Avklaringkode(
            kode = "EØSArbeid",
            tittel = "Sjekk om arbeid i EØS fører til sammenlegging",
            beskrivelse = "Personen har oppgitt arbeid fra EØS i søknaden. Det må vurderes om det skal være sammenlegging.",
        )

    val JobbetUtenforNorge =
        Avklaringkode(
            kode = "JobbetUtenforNorge",
            tittel = "Sjekk om arbeid utenfor Norge påvirker retten til dagpenger",
            beskrivelse =
                """
                Personen har oppgitt arbeid utenfor Norge i søknaden. Sjekk om arbeidsforholdene som er oppgitt i søknaden skal være 
                med i vurderingen av retten til dagpenger.
                """.trimIndent(),
        )

    val HattLukkedeSakerSiste8Uker =
        Avklaringkode(
            kode = "HattLukkedeSakerSiste8Uker",
            tittel = "Sjekk om nylig lukkede saker i Arena kan påvirke behandlingen",
            beskrivelse =
                """
                Personen har lukkede saker i Arena siste 8 uker. Har vi nylig gitt avslag bør vi sjekke om det er nødvendig med ekstra 
                veiledning.
                """.trimIndent(),
        )

    val SøktGjenopptak =
        Avklaringkode(
            kode = "SøktGjenopptak",
            tittel = "Bruker har søkt om gjenopptak",
            beskrivelse = "Personen har søkt om gjenopptak. Saker som skal gjenopptas må håndteres i Arena.",
        )

    val MuligGjenopptak =
        Avklaringkode(
            kode = "MuligGjenopptak",
            tittel = "Sjekk om det er sak som kan gjenopptas i Arena",
            beskrivelse = "Personen har åpne saker i Arena som kan være gjenopptak. Saker som skal gjenopptas må håndteres i Arena.",
        )

    val GjenopptakBehandling =
        Avklaringkode(
            kode = "GjenopptakBehandling",
            tittel = "!! Behandles som gjenopptak i ny løsning. Disse støtter vi ikke, så IKKE RØR 😬",
            beskrivelse = "Denne saken har en innvilget behandling i ny løsning, og det må vurderes om den skal gjenopptas.",
        )

    val InntektNesteKalendermåned =
        Avklaringkode(
            kode = "InntektNesteKalendermåned",
            tittel = "Sjekk om inntekt for neste måned er relevant",
            beskrivelse =
                """
                Personen har inntekt som tilhører neste inntektsperiode. Vurder om det er tilstrekkelige inntekter til at utfallet vil 
                endre seg i neste inntektsperiode.
                """.trimIndent(),
        )

    val SvangerskapsrelaterteSykepenger =
        Avklaringkode(
            kode = "SvangerskapsrelaterteSykepenger",
            tittel = "Sjekk om søker har fått sykepenger på grunn av svangerskap som skal med i minsteinntekt",
            beskrivelse =
                """
                Personen har fått utbetalt sykepenger. Om det er svangerskapsrelaterte sykepenger skal være med i inntektstgrunnlaget for 
                vurderingen av minste arbeidsinntekt.
                """.trimIndent(),
        )

    val PrøvingsdatoEtterRapporteringsfrist =
        Avklaringkode(
            kode = "PrøvingsdatoEtterRapporteringsfrist",
            tittel = "Sjekk om behandlingen bør ventes til etter A-ordningens rapporteringsfrist",
            beskrivelse =
                """
                Prøvingsdatoen er innenfor neste rapporteringsperiode for inntekt. 
                Vurder om du bør vente til etter fristen for å få med korrekte inntekter.
                """.trimIndent(),
        )

    val BrukerUnder18 =
        Avklaringkode(
            kode = "BrukerUnder18",
            tittel = "Bruker er under 18",
            beskrivelse = "Bruker er under 18 og skal ikke ha automatisk behandling",
        )

    val BarnMåGodkjennes =
        Avklaringkode(
            kode = "BarnMåGodkjennes",
            tittel = "Sjekk hvilke barn som skal gi barnetillegg",
            beskrivelse = "Barn må godkjennes om de skal gi barnetillegg",
        )

    val ReellArbeidssøkerUnntak =
        Avklaringkode(
            kode = "ReellArbeidssøkerUnntak",
            tittel = "Sjekk om søker oppfyller unntak til å være reell arbeidssøker",
            beskrivelse = "Det må vurderes om søker kvalifiserer til unntakene til reell arbeidssøker",
        )

    val IkkeRegistrertSomArbeidsøker =
        Avklaringkode(
            kode = "IkkeRegistrertSomArbeidsøker",
            tittel = "Søker er ikke registrert som arbeidssøker",
            beskrivelse = "Søker er ikke registrert som arbeidssøker.",
        )

    val HarOppgittPermittering =
        Avklaringkode(
            kode = "HarOppgittPermittering",
            tittel = "Sjekk om bruker skal ha dagpenger som permittert",
            beskrivelse = "Du må vurdere om bruker er permittert og oppfyller kravene til permittering",
        )

    val HarTilleggsopplysninger =
        Avklaringkode(
            kode = "HarTilleggsopplysninger",
            tittel = "Sjekk hva bruker har oppgitt som tilleggsopplysninger i søknaden",
            beskrivelse = "Du må vurdere om tilleggsopplysninger har betydning for saken",
        )

    val HarOppgittPermitteringFiskeindustri =
        Avklaringkode(
            kode = "HarOppgittPermitteringFiskeindustri",
            tittel = "Sjekk om bruker skal ha dagpenger som permittert fra fiskeindustrien",
            beskrivelse = "Du må vurdere om bruker er permittert og oppfyller kravene til permittering fra fiskeindustrien",
        )

    val GrunnbeløpForGrunnlagEndret =
        Avklaringkode(
            kode = "NyttGrunnbeløpForGrunnlag",
            tittel = "Grunnbeløpet for dagpengegrunnlag kan være utdatert",
            beskrivelse =
                """
                Prøvingsdatoen er 1. mai eller senere. Grunnbeløpet for inneværende år var ikke iverksatt på behandlingstidspunktet.
                Hvis grunnbeløpet ikke er vedtatt enda kan behandlingen godkjennes med det gamle grunnbeløpet. Det blir G-justert i Arena.
                Er grunnbeløpet for inneværende år vedtatt, kjør behandlingen på nytt og huk av 'Grunnbeløp for grunnlag' for å oppdatere grunnbeløpet.
                """.trimIndent(),
        )

    val Bostedsland =
        Avklaringkode(
            kode = "Bostedsland",
            tittel = "Bruker har oppgitt bostedsland som ikke er Norge",
            beskrivelse =
                """
                Du må sjekke om bruker oppfyller vilkåret om opphold i Norge eller er unntatt fra vilkåret om opphold
                """.trimIndent(),
        )

    val ManglerReellArbeidssøker =
        Avklaringkode(
            kode = "ManglerReellArbeidssøker",
            tittel = "Vurderingen av reell arbeidssøker mangler og utfallet er innvilgelse",
            beskrivelse =
                """
                Du må sørge for at vurderingen av reell arbeidssøker er utført før du kan innvilge 
                """.trimIndent(),
        )
}
