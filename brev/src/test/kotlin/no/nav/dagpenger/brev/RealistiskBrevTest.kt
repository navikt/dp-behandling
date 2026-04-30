package no.nav.dagpenger.brev

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.dagpenger.behandling.api.models.AvgjørelseDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsperiodeDTO
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.RettighetsperiodeDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

/**
 * Tester som demonstrerer realistiske brevmaler basert på faktiske dagpengebrev.
 */
class RealistiskBrevTest {
    // Opplysningstype-IDer (simulerer kjente vilkår og fastsettelser)
    private val kravTilAlderId = UUID.randomUUID()
    private val kravTilMinsteinntektId = UUID.randomUUID()
    private val kravTilReellArbeidssøkerId = UUID.randomUUID()
    private val kravTilTapAvArbeidstidId = UUID.randomUUID()
    private val dagsatsId = UUID.randomUUID()
    private val egenandelId = UUID.randomUUID()
    private val dagpengeperiodeId = UUID.randomUUID()
    private val vanligArbeidstidId = UUID.randomUUID()
    private val grunnlagId = UUID.randomUUID()

    private val harLøpendeRettId = UUID.randomUUID()

    // -- Brevmal som matcher innvilgelsesbrevet --
    private val innvilgelsesmal =
        Brevmal(
            navn = "Dagpenger - ny søknad",
            maltekster =
                listOf(
                    // Overskrift
                    Maltekst(
                        trigger = Trigger.Avgjørelse("Innvilgelse"),
                        tekst = "Nav har innvilget søknaden din om dagpenger",
                        plassering = Plassering.OVERSKRIFT,
                        rekkefølge = 1,
                    ),
                    Maltekst(
                        trigger = Trigger.Avgjørelse("Avslag"),
                        tekst = "Nav har avslått søknaden din om dagpenger",
                        plassering = Plassering.OVERSKRIFT,
                        rekkefølge = 1,
                    ),
                    // Innledning — åpen periode (bare fraOgMed)
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(harLøpendeRettId, periodeType = PeriodeType.ÅPEN),
                        tekst = "Du får dagpenger fra og med {{Har løpende rett.fraOgMed}}.",
                        plassering = Plassering.INNLEDNING,
                        rekkefølge = 1,
                    ),
                    // Innledning — lukket periode (fraOgMed + tilOgMed)
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(harLøpendeRettId, periodeType = PeriodeType.LUKKET),
                        tekst =
                            "Du får dagpenger fra og med {{Har løpende rett.fraOgMed}} " +
                                "til og med {{Har løpende rett.tilOgMed}}.",
                        plassering = Plassering.INNLEDNING,
                        rekkefølge = 1,
                    ),
                    // Innledning — flere perioder (saksbehandler må skrive selv)
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(harLøpendeRettId, periodeType = PeriodeType.FLERE),
                        tekst =
                            "[Saksbehandler: Rettighetsperioden har flere perioder som " +
                                "ikke kan beskrives maskinelt. Vennligst beskriv periodene manuelt.]",
                        plassering = Plassering.INNLEDNING,
                        rekkefølge = 1,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(dagsatsId),
                        tekst = "Du får {{Dagsats}} kroner dagen for fem dager i uken.",
                        plassering = Plassering.INNLEDNING,
                        rekkefølge = 2,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(egenandelId),
                        tekst = "Nav trekker en egenandel av dagpengene dine. Egenandelen din er {{Egenandel}} kroner.",
                        plassering = Plassering.INNLEDNING,
                        rekkefølge = 3,
                    ),
                    // Begrunnelse - vilkår som ble vurdert (kun nye)
                    Maltekst(
                        trigger = Trigger.OpplysningVerdi(kravTilAlderId, "true", kunNyeOpplysninger = true),
                        tittel = "Du oppfyller kravet til alder",
                        tekst = "Vurderingen er gjort etter folketrygdloven § 4-23.",
                        plassering = Plassering.VILKÅR,
                        rekkefølge = 1,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningVerdi(kravTilMinsteinntektId, "true", kunNyeOpplysninger = true),
                        tittel = "Du oppfyller kravet til minsteinntekt",
                        tekst = "Vurderingen er gjort etter folketrygdloven § 4-4.",
                        plassering = Plassering.VILKÅR,
                        rekkefølge = 2,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningVerdi(kravTilReellArbeidssøkerId, "true", kunNyeOpplysninger = true),
                        tittel = "Du er reell arbeidssøker",
                        tekst = "Vedtaket er gjort etter folketrygdloven § 4-5.",
                        plassering = Plassering.VILKÅR,
                        rekkefølge = 3,
                    ),
                    // Fastsettelser
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(dagpengeperiodeId),
                        tittel = "Hvor lenge kan du få dagpenger?",
                        tekst =
                            "Arbeidsinntekten din gir deg rett til en periode på maksimalt " +
                                "{{Dagpengeperiode}} uker med dagpenger. Vurderingen er gjort etter folketrygdloven § 4-15.",
                        plassering = Plassering.FASTSETTELSE,
                        rekkefølge = 1,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(dagsatsId),
                        tittel = "Slik har vi beregnet dagpengene dine",
                        tekst =
                            "Du får {{Dagsats}} kroner per dag for fem dager i uken. " +
                                "Inntektsgrunnlaget ditt er beregnet til {{Grunnlag}} kroner. " +
                                "Beregningen er gjort etter folketrygdloven § 4-11 andre ledd.",
                        plassering = Plassering.FASTSETTELSE,
                        rekkefølge = 2,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(vanligArbeidstidId),
                        tittel = "Arbeidstiden din",
                        tekst = "Vi har kommet frem til at den vanlige arbeidstiden din er {{Vanlig arbeidstid}} timer per uke.",
                        plassering = Plassering.FASTSETTELSE,
                        rekkefølge = 3,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningFinnes(egenandelId),
                        tittel = "Egenandel",
                        tekst =
                            "Egenandelen din er {{Egenandel}} kroner. " +
                                "Vi trekker egenandelen fra den første utbetalingen din. " +
                                "Les mer om egenandel i folketrygdloven § 4-9.",
                        plassering = Plassering.FASTSETTELSE,
                        rekkefølge = 4,
                    ),
                    // Informasjon (faste tekster)
                    Maltekst(
                        trigger = Trigger.Alltid,
                        tittel = "Du må sende meldekort",
                        tekst = "For å ha rett på dagpenger må du sende meldekort hver 14. dag.",
                        plassering = Plassering.INFORMASJON,
                        rekkefølge = 1,
                    ),
                    Maltekst(
                        trigger = Trigger.Alltid,
                        tittel = "Du må melde fra om endringer",
                        tekst = "Hvis det skjer en endring i situasjonen din, kan det påvirke dagpengene dine.",
                        plassering = Plassering.INFORMASJON,
                        rekkefølge = 2,
                    ),
                ),
        )

    @Test
    fun `innvilgelsesbrev med vilkår og fastsettelser`() {
        val resultat =
            lagResultat(
                AvgjørelseDTO.INNVILGELSE,
                opplysninger =
                    listOf(
                        lagOpplysning(kravTilAlderId, "Oppfyller kravet til alder", BoolskVerdiDTO(true), OpprinnelseDTO.NY),
                        lagOpplysning(
                            kravTilMinsteinntektId,
                            "Oppfyller kravet til minsteinntekt",
                            BoolskVerdiDTO(true),
                            OpprinnelseDTO.NY,
                        ),
                        lagOpplysning(kravTilReellArbeidssøkerId, "Krav til arbeidssøker", BoolskVerdiDTO(true), OpprinnelseDTO.NY),
                        lagOpplysningMedPeriode(
                            harLøpendeRettId,
                            "Har løpende rett",
                            BoolskVerdiDTO(true),
                            fraOgMed = LocalDate.of(2026, 4, 23),
                            tilOgMed = LocalDate.of(2026, 4, 27),
                        ),
                        lagOpplysning(dagsatsId, "Dagsats", PengeVerdiDTO(BigDecimal("462"))),
                        lagOpplysning(egenandelId, "Egenandel", PengeVerdiDTO(BigDecimal("1386"))),
                        lagOpplysning(dagpengeperiodeId, "Dagpengeperiode", HeltallVerdiDTO(52)),
                        lagOpplysning(vanligArbeidstidId, "Vanlig arbeidstid", DesimaltallVerdiDTO(37.5)),
                        lagOpplysning(grunnlagId, "Grunnlag", PengeVerdiDTO(BigDecimal("192508"))),
                    ),
            )

        val brev = BrevBygger(innvilgelsesmal).bygg(resultat)

        brev.overskrift shouldBe "Nav har innvilget søknaden din om dagpenger"

        // Innledning
        val innledning = brev.seksjoner.filter { it.plassering == Plassering.INNLEDNING }
        innledning shouldHaveSize 1
        innledning[0].innhold[0] shouldBe "Du får dagpenger fra og med 23. april 2026 til og med 27. april 2026."
        innledning[0].innhold[1] shouldContain "462 kroner dagen"
        innledning[0].innhold[2] shouldContain "Egenandelen din er 1386 kroner"

        // Vilkår — hver med tittel
        val vilkår = brev.seksjoner.filter { it.plassering == Plassering.VILKÅR }
        vilkår shouldHaveSize 3
        vilkår[0].tittel shouldBe "Du oppfyller kravet til alder"
        vilkår[0].innhold[0] shouldContain "§ 4-23"
        vilkår[1].tittel shouldBe "Du oppfyller kravet til minsteinntekt"
        vilkår[1].innhold[0] shouldContain "§ 4-4"
        vilkår[2].tittel shouldBe "Du er reell arbeidssøker"
        vilkår[2].innhold[0] shouldContain "§ 4-5"

        // Fastsettelser — hver med tittel
        val fastsettelser = brev.seksjoner.filter { it.plassering == Plassering.FASTSETTELSE }
        fastsettelser shouldHaveSize 4
        fastsettelser[0].tittel shouldBe "Hvor lenge kan du få dagpenger?"
        fastsettelser[0].innhold[0] shouldContain "52 uker"
        fastsettelser[1].tittel shouldBe "Slik har vi beregnet dagpengene dine"
        fastsettelser[1].innhold[0] shouldContain "462 kroner per dag"
        fastsettelser[1].innhold[0] shouldContain "192508 kroner"
        fastsettelser[2].tittel shouldBe "Arbeidstiden din"
        fastsettelser[2].innhold[0] shouldContain "37.5 timer"
        fastsettelser[3].tittel shouldBe "Egenandel"

        // Informasjon — faste tekster
        val info = brev.seksjoner.filter { it.plassering == Plassering.INFORMASJON }
        info shouldHaveSize 2
        info[0].tittel shouldBe "Du må sende meldekort"
        info[1].tittel shouldBe "Du må melde fra om endringer"

        TypstRenderer.render(brev)
    }

    @Test
    fun `innvilgelsesbrev viser bare vilkår med nye perioder`() {
        val resultat =
            lagResultat(
                AvgjørelseDTO.INNVILGELSE,
                opplysninger =
                    listOf(
                        // Alder ble vurdert i denne behandlingen (NY)
                        lagOpplysning(kravTilAlderId, "Oppfyller kravet til alder", BoolskVerdiDTO(true), OpprinnelseDTO.NY),
                        // Minsteinntekt er arvet fra forrige behandling
                        lagOpplysning(
                            kravTilMinsteinntektId,
                            "Oppfyller kravet til minsteinntekt",
                            BoolskVerdiDTO(true),
                            OpprinnelseDTO.ARVET,
                        ),
                        lagOpplysningMedPeriode(
                            harLøpendeRettId,
                            "Har løpende rett",
                            BoolskVerdiDTO(true),
                            fraOgMed = LocalDate.of(2026, 4, 23),
                        ),
                        lagOpplysning(dagsatsId, "Dagsats", PengeVerdiDTO(BigDecimal("462"))),
                        lagOpplysning(dagpengeperiodeId, "Dagpengeperiode", HeltallVerdiDTO(52)),
                    ),
            )

        val brev = BrevBygger(innvilgelsesmal).bygg(resultat)

        // Åpen periode — bare fraOgMed
        val innledning = brev.seksjoner.filter { it.plassering == Plassering.INNLEDNING }
        innledning shouldHaveSize 1
        innledning[0].innhold[0] shouldBe "Du får dagpenger fra og med 23. april 2026."

        val vilkår = brev.seksjoner.filter { it.plassering == Plassering.VILKÅR }
        // Bare alder vises — minsteinntekt er arvet og filtreres bort (kunNyeOpplysninger=true)
        vilkår shouldHaveSize 1
        vilkår[0].tittel shouldBe "Du oppfyller kravet til alder"
    }

    // -- Brevmal for avslag --
    private val avslagsmal =
        Brevmal(
            navn = "Dagpenger - avslag",
            maltekster =
                listOf(
                    Maltekst(
                        trigger = Trigger.Avgjørelse("Avslag"),
                        tekst = "Nav har avslått søknaden din om dagpenger",
                        plassering = Plassering.OVERSKRIFT,
                        rekkefølge = 1,
                    ),
                    Maltekst(
                        trigger = Trigger.Avgjørelse("Avslag"),
                        tekst = "Vi har avslått søknaden din om dagpenger fra 23. april 2026.",
                        plassering = Plassering.INNLEDNING,
                        rekkefølge = 1,
                    ),
                    // Begrunnelse - et vilkår per seksjon
                    Maltekst(
                        trigger = Trigger.Avgjørelse("Avslag"),
                        tittel = "Derfor får du avslag",
                        tekst = "",
                        plassering = Plassering.BEGRUNNELSE,
                        rekkefølge = 0,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningVerdi(kravTilReellArbeidssøkerId, "false", kunNyeOpplysninger = true),
                        tittel = "Du må være reell arbeidssøker",
                        tekst =
                            "For å ha rett til dagpenger, må du være villig til å ta alle typer arbeid med vanlig lønn. " +
                                "Du har opplyst oss om at du ikke vil ta alle typer arbeid med vanlig lønn, " +
                                "derfor har du ikke rett til dagpenger. Vedtaket er gjort etter folketrygdloven § 4-5.",
                        plassering = Plassering.BEGRUNNELSE,
                        rekkefølge = 1,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningVerdi(kravTilMinsteinntektId, "false", kunNyeOpplysninger = true),
                        tittel = "Du oppfyller ikke kravet til minsteinntekt",
                        tekst = "Du har ikke hatt tilstrekkelig arbeidsinntekt. Vurderingen er gjort etter folketrygdloven § 4-4.",
                        plassering = Plassering.BEGRUNNELSE,
                        rekkefølge = 2,
                    ),
                    Maltekst(
                        trigger = Trigger.OpplysningVerdi(kravTilAlderId, "false", kunNyeOpplysninger = true),
                        tittel = "Du oppfyller ikke kravet til alder",
                        tekst = "Du oppfyller ikke alderskravet. Vurderingen er gjort etter folketrygdloven § 4-23.",
                        plassering = Plassering.BEGRUNNELSE,
                        rekkefølge = 3,
                    ),
                    // Faste avslutnings-tekster
                    Maltekst(
                        trigger = Trigger.Alltid,
                        tittel = "Du har rett til innsyn",
                        tekst =
                            "Kontakt oss om du vil se dokumentene i saken din. " +
                                "Ta kontakt på nav.no/kontakt eller på telefon 55 55 33 33.",
                        plassering = Plassering.AVSLUTNING,
                        rekkefølge = 1,
                    ),
                    Maltekst(
                        trigger = Trigger.Alltid,
                        tittel = "Du har rett til å få hjelp fra andre",
                        tekst = "Du kan be om hjelp fra andre under hele saksbehandlingen. Dette følger av forvaltningsloven § 12.",
                        plassering = Plassering.AVSLUTNING,
                        rekkefølge = 2,
                    ),
                ),
        )

    @Test
    fun `avslagsbrev med begrunnelse per vilkår som ikke er oppfylt`() {
        val resultat =
            lagResultat(
                AvgjørelseDTO.AVSLAG,
                opplysninger =
                    listOf(
                        lagOpplysning(kravTilAlderId, "Oppfyller kravet til alder", BoolskVerdiDTO(true), OpprinnelseDTO.NY),
                        lagOpplysning(
                            kravTilMinsteinntektId,
                            "Oppfyller kravet til minsteinntekt",
                            BoolskVerdiDTO(true),
                            OpprinnelseDTO.NY,
                        ),
                        lagOpplysning(kravTilReellArbeidssøkerId, "Krav til arbeidssøker", BoolskVerdiDTO(false), OpprinnelseDTO.NY),
                    ),
            )

        val brev = BrevBygger(avslagsmal).bygg(resultat)

        brev.overskrift shouldBe "Nav har avslått søknaden din om dagpenger"

        // Innledning
        val innledning = brev.seksjoner.filter { it.plassering == Plassering.INNLEDNING }
        innledning shouldHaveSize 1
        innledning[0].innhold[0] shouldContain "avslått"

        // Begrunnelse - bare vilkåret som ikke er oppfylt
        val begrunnelse = brev.seksjoner.filter { it.plassering == Plassering.BEGRUNNELSE }
        begrunnelse shouldHaveSize 2 // "Derfor får du avslag" + reell arbeidssøker
        begrunnelse[0].tittel shouldBe "Derfor får du avslag"
        begrunnelse[1].tittel shouldBe "Du må være reell arbeidssøker"
        begrunnelse[1].innhold[0] shouldContain "folketrygdloven § 4-5"

        // Avslutning - faste tekster
        val avslutning = brev.seksjoner.filter { it.plassering == Plassering.AVSLUTNING }
        avslutning shouldHaveSize 2
        avslutning[0].tittel shouldBe "Du har rett til innsyn"
        avslutning[1].tittel shouldBe "Du har rett til å få hjelp fra andre"
    }

    @Test
    fun `avslagsbrev med flere vilkår som ikke er oppfylt`() {
        val resultat =
            lagResultat(
                AvgjørelseDTO.AVSLAG,
                opplysninger =
                    listOf(
                        lagOpplysning(kravTilAlderId, "Oppfyller kravet til alder", BoolskVerdiDTO(false), OpprinnelseDTO.NY),
                        lagOpplysning(
                            kravTilMinsteinntektId,
                            "Oppfyller kravet til minsteinntekt",
                            BoolskVerdiDTO(false),
                            OpprinnelseDTO.NY,
                        ),
                        lagOpplysning(kravTilReellArbeidssøkerId, "Krav til arbeidssøker", BoolskVerdiDTO(true), OpprinnelseDTO.NY),
                    ),
            )

        val brev = BrevBygger(avslagsmal).bygg(resultat)

        val begrunnelse = brev.seksjoner.filter { it.plassering == Plassering.BEGRUNNELSE }
        // "Derfor får du avslag" + minsteinntekt + alder (arbeidssøker er oppfylt, vises ikke)
        begrunnelse shouldHaveSize 3
        begrunnelse[0].tittel shouldBe "Derfor får du avslag"
        begrunnelse[1].tittel shouldBe "Du oppfyller ikke kravet til minsteinntekt"
        begrunnelse[2].tittel shouldBe "Du oppfyller ikke kravet til alder"
    }

    @Test
    fun `innvilgelsesbrev med flere perioder gir saksbehandler-placeholder`() {
        val resultat =
            lagResultat(
                AvgjørelseDTO.INNVILGELSE,
                opplysninger =
                    listOf(
                        lagOpplysningMedFlerePerioder(
                            harLøpendeRettId,
                            "Har løpende rett",
                            listOf(
                                LocalDate.of(2026, 1, 1) to LocalDate.of(2026, 3, 31),
                                LocalDate.of(2026, 5, 1) to LocalDate.of(2026, 6, 30),
                            ),
                        ),
                        lagOpplysning(dagsatsId, "Dagsats", PengeVerdiDTO(BigDecimal("462"))),
                    ),
            )

        val brev = BrevBygger(innvilgelsesmal).bygg(resultat)

        val innledning = brev.seksjoner.filter { it.plassering == Plassering.INNLEDNING }
        innledning shouldHaveSize 1
        innledning[0].innhold[0] shouldContain "[Saksbehandler:"
        innledning[0].innhold[0] shouldContain "ikke kan beskrives maskinelt"
    }

    // -- Hjelpemetoder --

    private fun lagResultat(
        avgjørelse: AvgjørelseDTO,
        opplysninger: List<OpplysningerDTO> = emptyList(),
    ) = BehandlingsresultatDTO(
        behandlingId = UUID.randomUUID(),
        behandletHendelse =
            HendelseDTO(
                id = UUID.randomUUID().toString(),
                datatype = "Søknad",
                type = HendelseDTOTypeDTO.SØKNAD,
                skjedde = LocalDate.of(2026, 4, 23),
            ),
        behandlingskjedeId = UUID.randomUUID(),
        automatisk = true,
        ident = "12345678910",
        rettighetsperioder =
            listOf(
                RettighetsperiodeDTO(
                    fraOgMed = LocalDate.of(2026, 4, 23),
                    tilOgMed = LocalDate.of(2026, 4, 27),
                    harRett = avgjørelse == AvgjørelseDTO.INNVILGELSE,
                ),
            ),
        opprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        opplysninger = opplysninger,
        utbetalinger = emptyList(),
        behandletAv = emptyList(),
        førteTil = avgjørelse,
    )

    private fun lagOpplysning(
        typeId: UUID,
        navn: String,
        verdi: no.nav.dagpenger.behandling.api.models.OpplysningsverdiDTO,
        opprinnelse: OpprinnelseDTO = OpprinnelseDTO.NY,
    ) = OpplysningerDTO(
        opplysningTypeId = typeId,
        navn = navn,
        datatype = DataTypeDTO.BOOLSK,
        perioder =
            listOf(
                OpplysningsperiodeDTO(
                    id = UUID.randomUUID(),
                    opprettet = LocalDateTime.now(),
                    opprinnelse = opprinnelse,
                    verdi = verdi,
                ),
            ),
    )

    private fun lagOpplysningMedPeriode(
        typeId: UUID,
        navn: String,
        verdi: no.nav.dagpenger.behandling.api.models.OpplysningsverdiDTO,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate? = null,
        opprinnelse: OpprinnelseDTO = OpprinnelseDTO.NY,
    ) = OpplysningerDTO(
        opplysningTypeId = typeId,
        navn = navn,
        datatype = DataTypeDTO.BOOLSK,
        perioder =
            listOf(
                OpplysningsperiodeDTO(
                    id = UUID.randomUUID(),
                    opprettet = LocalDateTime.now(),
                    opprinnelse = opprinnelse,
                    verdi = verdi,
                    gyldigFraOgMed = fraOgMed,
                    gyldigTilOgMed = tilOgMed,
                ),
            ),
    )

    private fun lagOpplysningMedFlerePerioder(
        typeId: UUID,
        navn: String,
        perioder: List<Pair<LocalDate, LocalDate>>,
        opprinnelse: OpprinnelseDTO = OpprinnelseDTO.NY,
    ) = OpplysningerDTO(
        opplysningTypeId = typeId,
        navn = navn,
        datatype = DataTypeDTO.BOOLSK,
        perioder =
            perioder.map { (fra, til) ->
                OpplysningsperiodeDTO(
                    id = UUID.randomUUID(),
                    opprettet = LocalDateTime.now(),
                    opprinnelse = opprinnelse,
                    verdi = BoolskVerdiDTO(true),
                    gyldigFraOgMed = fra,
                    gyldigTilOgMed = til,
                )
            },
    )
}
