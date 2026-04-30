package no.nav.dagpenger.brev

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.dagpenger.behandling.api.models.AvgjørelseDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
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

class BrevByggerTest {
    private val kravTilAlderId = UUID.randomUUID()
    private val kravTilMinsteinntektId = UUID.randomUUID()
    private val dagsatsId = UUID.randomUUID()
    private val grunnlagId = UUID.randomUUID()

    @Test
    fun `innvilgelsesbrev med faste elementer og avgjørelses-trigger`() {
        val brevmal =
            Brevmal(
                navn = "Dagpengebrev",
                maltekster =
                    listOf(
                        maltekst(Trigger.Avgjørelse("Innvilgelse"), "Innvilgelse av dagpenger", Plassering.OVERSKRIFT, 1),
                        maltekst(Trigger.Avgjørelse("Avslag"), "Avslag på søknad om dagpenger", Plassering.OVERSKRIFT, 1),
                        maltekst(Trigger.Alltid, "Vi har behandlet søknaden din om dagpenger.", Plassering.INNLEDNING, 1),
                        maltekst(Trigger.Avgjørelse("Innvilgelse"), "Du har fått innvilget dagpenger.", Plassering.INNLEDNING, 2),
                        maltekst(
                            Trigger.Alltid,
                            "Du kan klage på vedtaket innen 6 uker.",
                            Plassering.AVSLUTNING,
                            1,
                        ),
                    ),
            )

        val resultat = lagResultat(AvgjørelseDTO.INNVILGELSE)
        val brev = BrevBygger(brevmal).bygg(resultat)!!

        brev.overskrift shouldBe "Innvilgelse av dagpenger"
        brev.seksjoner shouldHaveSize 2

        val innledning = brev.seksjoner.first { it.plassering == Plassering.INNLEDNING }
        innledning.innhold shouldHaveSize 2
        innledning.innhold[0] shouldBe "Vi har behandlet søknaden din om dagpenger."
        innledning.innhold[1] shouldBe "Du har fått innvilget dagpenger."

        val avslutning = brev.seksjoner.first { it.plassering == Plassering.AVSLUTNING }
        avslutning.innhold[0] shouldBe "Du kan klage på vedtaket innen 6 uker."
    }

    @Test
    fun `avslagsbrev viser riktig overskrift`() {
        val brevmal =
            Brevmal(
                navn = "Dagpengebrev",
                maltekster =
                    listOf(
                        maltekst(Trigger.Avgjørelse("Innvilgelse"), "Innvilgelse av dagpenger", Plassering.OVERSKRIFT, 1),
                        maltekst(Trigger.Avgjørelse("Avslag"), "Avslag på søknad om dagpenger", Plassering.OVERSKRIFT, 1),
                    ),
            )

        val resultat = lagResultat(AvgjørelseDTO.AVSLAG)
        val brev = BrevBygger(brevmal).bygg(resultat)!!

        brev.overskrift shouldBe "Avslag på søknad om dagpenger"
    }

    @Test
    fun `interpolering av opplysningsverdier i maltekst`() {
        val brevmal =
            Brevmal(
                navn = "Dagpengebrev",
                maltekster =
                    listOf(
                        maltekst(Trigger.Alltid, "{{Avgjørelse}} av dagpenger", Plassering.OVERSKRIFT, 1),
                        maltekst(
                            Trigger.Alltid,
                            "Din dagsats er {{Dagsats}} kroner, basert på et grunnlag på {{Grunnlag}} kroner.",
                            Plassering.FASTSETTELSE,
                            1,
                        ),
                    ),
            )

        val resultat =
            lagResultat(
                AvgjørelseDTO.INNVILGELSE,
                opplysninger =
                    listOf(
                        lagOpplysning(dagsatsId, "Dagsats", PengeVerdiDTO(verdi = BigDecimal("1234"))),
                        lagOpplysning(grunnlagId, "Grunnlag", PengeVerdiDTO(verdi = BigDecimal("350000"))),
                    ),
            )

        val brev = BrevBygger(brevmal).bygg(resultat)!!

        brev.overskrift shouldBe "Innvilgelse av dagpenger"

        val fastsettelse = brev.seksjoner.first { it.plassering == Plassering.FASTSETTELSE }
        fastsettelse.innhold[0] shouldBe "Din dagsats er 1234 kroner, basert på et grunnlag på 350000 kroner."
    }

    @Test
    fun `trigger basert på tilstedeværelse av opplysning`() {
        val brevmal =
            Brevmal(
                navn = "Dagpengebrev",
                maltekster =
                    listOf(
                        maltekst(Trigger.Alltid, "Overskrift", Plassering.OVERSKRIFT, 1),
                        maltekst(
                            Trigger.OpplysningFinnes(kravTilMinsteinntektId),
                            "Vi har vurdert din inntekt.",
                            Plassering.VILKÅR,
                            1,
                        ),
                        maltekst(
                            Trigger.OpplysningFinnes(UUID.randomUUID()),
                            "Denne teksten skal ikke vises.",
                            Plassering.VILKÅR,
                            2,
                        ),
                    ),
            )

        val resultat =
            lagResultat(
                AvgjørelseDTO.INNVILGELSE,
                opplysninger = listOf(lagOpplysning(kravTilMinsteinntektId, "Minsteinntekt", BoolskVerdiDTO(verdi = true))),
            )

        val brev = BrevBygger(brevmal).bygg(resultat)!!

        val vilkår = brev.seksjoner.first { it.plassering == Plassering.VILKÅR }
        vilkår.innhold shouldHaveSize 1
        vilkår.innhold[0] shouldBe "Vi har vurdert din inntekt."
    }

    @Test
    fun `trigger basert på verdien av en opplysning`() {
        val brevmal =
            Brevmal(
                navn = "Dagpengebrev",
                maltekster =
                    listOf(
                        maltekst(Trigger.Alltid, "Overskrift", Plassering.OVERSKRIFT, 1),
                        maltekst(
                            Trigger.OpplysningVerdi(kravTilAlderId, "true"),
                            "Du oppfyller kravet til alder.",
                            Plassering.VILKÅR,
                            1,
                        ),
                        maltekst(
                            Trigger.OpplysningVerdi(kravTilAlderId, "false"),
                            "Du oppfyller ikke kravet til alder.",
                            Plassering.VILKÅR,
                            2,
                        ),
                    ),
            )

        val resultat =
            lagResultat(
                AvgjørelseDTO.AVSLAG,
                opplysninger = listOf(lagOpplysning(kravTilAlderId, "Oppfyller kravet til alder", BoolskVerdiDTO(verdi = false))),
            )

        val brev = BrevBygger(brevmal).bygg(resultat)!!

        val vilkår = brev.seksjoner.first { it.plassering == Plassering.VILKÅR }
        vilkår.innhold shouldHaveSize 1
        vilkår.innhold[0] shouldBe "Du oppfyller ikke kravet til alder."
    }

    @Test
    fun `komplett brev med alle trigger-typer`() {
        val brevmal =
            Brevmal(
                navn = "Dagpengebrev",
                maltekster =
                    listOf(
                        maltekst(Trigger.Avgjørelse("Innvilgelse"), "Innvilgelse av dagpenger", Plassering.OVERSKRIFT, 1),
                        maltekst(Trigger.Alltid, "Vi har behandlet søknaden din om dagpenger.", Plassering.INNLEDNING, 1),
                        maltekst(
                            Trigger.OpplysningVerdi(kravTilAlderId, "true"),
                            "Du oppfyller kravet til alder.",
                            Plassering.VILKÅR,
                            1,
                        ),
                        maltekst(
                            Trigger.OpplysningVerdi(kravTilMinsteinntektId, "true"),
                            "Du oppfyller kravet til minsteinntekt.",
                            Plassering.VILKÅR,
                            2,
                        ),
                        maltekst(
                            Trigger.OpplysningFinnes(dagsatsId),
                            "Din dagsats er {{Dagsats}} kroner.",
                            Plassering.FASTSETTELSE,
                            1,
                        ),
                        maltekst(Trigger.Alltid, "Med vennlig hilsen\nNAV", Plassering.AVSLUTNING, 1),
                    ),
            )

        val resultat =
            lagResultat(
                AvgjørelseDTO.INNVILGELSE,
                opplysninger =
                    listOf(
                        lagOpplysning(kravTilAlderId, "Oppfyller kravet til alder", BoolskVerdiDTO(verdi = true)),
                        lagOpplysning(kravTilMinsteinntektId, "Oppfyller kravet til minsteinntekt", BoolskVerdiDTO(verdi = true)),
                        lagOpplysning(dagsatsId, "Dagsats", PengeVerdiDTO(verdi = BigDecimal("1234"))),
                    ),
            )

        val brev = BrevBygger(brevmal).bygg(resultat)!!

        brev.overskrift shouldBe "Innvilgelse av dagpenger"
        brev.seksjoner shouldHaveSize 4

        val innledning = brev.seksjoner.first { it.plassering == Plassering.INNLEDNING }
        innledning.innhold[0] shouldBe "Vi har behandlet søknaden din om dagpenger."

        val vilkår = brev.seksjoner.first { it.plassering == Plassering.VILKÅR }
        vilkår.innhold shouldHaveSize 2
        vilkår.innhold[0] shouldBe "Du oppfyller kravet til alder."
        vilkår.innhold[1] shouldBe "Du oppfyller kravet til minsteinntekt."

        val fastsettelse = brev.seksjoner.first { it.plassering == Plassering.FASTSETTELSE }
        fastsettelse.innhold[0] shouldBe "Din dagsats er 1234 kroner."

        val avslutning = brev.seksjoner.first { it.plassering == Plassering.AVSLUTNING }
        avslutning.innhold[0] shouldContain "Med vennlig hilsen"
    }

    private fun maltekst(
        trigger: Trigger,
        tekst: String,
        plassering: Plassering,
        rekkefølge: Int,
    ) = Maltekst(
        trigger = trigger,
        tekst = tekst,
        plassering = plassering,
        rekkefølge = rekkefølge,
    )

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
                skjedde = LocalDate.now(),
            ),
        behandlingskjedeId = UUID.randomUUID(),
        automatisk = true,
        ident = "12345678910",
        rettighetsperioder =
            listOf(
                RettighetsperiodeDTO(
                    fraOgMed = LocalDate.now(),
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
    ) = OpplysningerDTO(
        opplysningTypeId = typeId,
        navn = navn,
        datatype = DataTypeDTO.BOOLSK,
        perioder =
            listOf(
                OpplysningsperiodeDTO(
                    id = UUID.randomUUID(),
                    opprettet = LocalDateTime.now(),
                    opprinnelse = OpprinnelseDTO.NY,
                    verdi = verdi,
                ),
            ),
    )
}
