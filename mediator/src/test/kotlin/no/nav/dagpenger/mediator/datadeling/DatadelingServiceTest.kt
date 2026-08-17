package no.nav.dagpenger.mediator.datadeling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.api.models.DatadelingForesporselDTO
import no.nav.dagpenger.mediator.api.models.YtelsestypeDTO
import no.nav.dagpenger.mediator.repository.BehandlingMedOpplysninger
import no.nav.dagpenger.mediator.repository.DatadelingRepository
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DatadelingServiceTest {
    private val mandag = LocalDate.of(2025, 1, 6)
    private val tirsdag = LocalDate.of(2025, 1, 7)

    private class StubRepository(
        private val behandlinger: List<BehandlingMedOpplysninger>,
    ) : DatadelingRepository {
        override fun hentFerdigeBehandlinger(ident: String) = behandlinger
    }

    private fun behandlingMed(vararg opplysninger: Opplysning<*>) =
        BehandlingMedOpplysninger(
            behandlingId = UUID.randomUUID(),
            opplysninger = Opplysninger.med(*opplysninger),
        )

    private fun harLøpendeRett(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate = LocalDate.MAX,
    ) = Faktum(KravPåDagpenger.harLøpendeRett, true, Gyldighetsperiode(fraOgMed, tilOgMed))

    @Test
    fun `perioder bygges fra harLøpendeRett-opplysninger`() {
        val service =
            DatadelingService(
                StubRepository(
                    listOf(behandlingMed(harLøpendeRett(fraOgMed = mandag))),
                ),
            )

        val response = service.hentPerioder(DatadelingForesporselDTO("12345678910", mandag, tirsdag))

        response.perioder shouldHaveSize 1
        with(response.perioder.single()) {
            fraOgMed shouldBe mandag
            tilOgMed shouldBe null
            harRett shouldBe true
            ytelseType shouldBe YtelsestypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER
        }
    }

    @Test
    fun `perioder uten rett returneres med harRett false`() {
        val service =
            DatadelingService(
                StubRepository(
                    listOf(
                        behandlingMed(
                            Faktum(KravPåDagpenger.harLøpendeRett, false, Gyldighetsperiode(mandag, tirsdag)),
                        ),
                    ),
                ),
            )

        val response = service.hentPerioder(DatadelingForesporselDTO("12345678910", mandag, tirsdag))

        response.perioder shouldHaveSize 1
        with(response.perioder.single()) {
            fraOgMed shouldBe mandag
            tilOgMed shouldBe tirsdag
            harRett shouldBe false
        }
    }

    @Test
    fun `perioder utenfor forespørselsperioden filtreres bort`() {
        val service =
            DatadelingService(
                StubRepository(
                    listOf(behandlingMed(harLøpendeRett(fraOgMed = mandag.minusMonths(2), tilOgMed = mandag.minusMonths(1)))),
                ),
            )

        service.hentPerioder(DatadelingForesporselDTO("12345678910", mandag, tirsdag)).perioder shouldHaveSize 0
    }

    @Test
    fun `ytelsestype utledes fra rettighetstype-opplysning`() {
        val permittert = boolsk(OpplysningsTyper.PermittertId, "Bruker er permittert")
        val service =
            DatadelingService(
                StubRepository(
                    listOf(
                        behandlingMed(
                            harLøpendeRett(fraOgMed = mandag),
                            Faktum(permittert, true, Gyldighetsperiode(mandag, LocalDate.MAX)),
                        ),
                    ),
                ),
            )

        val response = service.hentPerioder(DatadelingForesporselDTO("12345678910", mandag))

        response.perioder.single().ytelseType shouldBe YtelsestypeDTO.DAGPENGER_PERMITTERING_ORDINAER
    }

    @Test
    fun `beregninger bygges fra utbetalingsopplysninger`() {
        val service =
            DatadelingService(
                StubRepository(
                    listOf(
                        behandlingMed(
                            harLøpendeRett(fraOgMed = mandag),
                            Faktum(Beregning.meldeperiode, Periode(mandag, tirsdag), Gyldighetsperiode(mandag, tirsdag)),
                            Faktum(
                                DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg,
                                Beløp(600),
                                Gyldighetsperiode(mandag, LocalDate.MAX),
                            ),
                            Faktum(Beregning.utbetaling, Beløp(500), Gyldighetsperiode(mandag, mandag)),
                            Faktum(Beregning.utbetaling, Beløp(500), Gyldighetsperiode(tirsdag, tirsdag)),
                            Faktum(Beregning.gjenståendeDager, 99, Gyldighetsperiode(mandag, mandag)),
                            Faktum(Beregning.gjenståendeDager, 98, Gyldighetsperiode(tirsdag, tirsdag)),
                        ),
                    ),
                ),
            )

        val beregninger = service.hentBeregninger(DatadelingForesporselDTO("12345678910", mandag, tirsdag))

        beregninger shouldHaveSize 2
        with(beregninger.first()) {
            fraOgMed shouldBe mandag
            tilOgMed shouldBe mandag
            sats shouldBe 600
            utbetaltBeløp shouldBe 500
            gjenståendeDager shouldBe 99
        }
    }

    @Test
    fun `gjenstående dager faller tilbake til innvilget antall stønadsdager`() {
        val service =
            DatadelingService(
                StubRepository(
                    listOf(
                        behandlingMed(
                            harLøpendeRett(fraOgMed = mandag),
                            Faktum(Beregning.meldeperiode, Periode(mandag, mandag), Gyldighetsperiode(mandag, mandag)),
                            Faktum(
                                DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg,
                                Beløp(600),
                                Gyldighetsperiode(mandag, LocalDate.MAX),
                            ),
                            Faktum(Beregning.utbetaling, Beløp(500), Gyldighetsperiode(mandag, mandag)),
                            Faktum(Dagpengeperiode.antallStønadsdager, 260, Gyldighetsperiode(mandag, LocalDate.MAX)),
                        ),
                    ),
                ),
            )

        val beregninger = service.hentBeregninger(DatadelingForesporselDTO("12345678910", mandag, mandag))

        beregninger.single().gjenståendeDager shouldBe 260
    }
}
