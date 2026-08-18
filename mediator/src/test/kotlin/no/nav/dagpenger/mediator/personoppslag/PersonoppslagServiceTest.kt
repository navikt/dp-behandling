package no.nav.dagpenger.mediator.personoppslag

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.api.models.YtelsestypeDTO
import no.nav.dagpenger.mediator.repository.BehandlingskjedeOpplysninger
import no.nav.dagpenger.mediator.repository.PersonOpplysningerRepository
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

class PersonoppslagServiceTest {
    private val mandag = LocalDate.of(2025, 1, 6)
    private val tirsdag = LocalDate.of(2025, 1, 7)

    private class StubRepository(
        private val kjedelag: List<BehandlingskjedeOpplysninger>,
    ) : PersonOpplysningerRepository {
        override fun hentRelevanteOpplysninger(ident: String) = kjedelag
    }

    private fun kjedeMed(vararg opplysninger: Opplysning<*>) =
        BehandlingskjedeOpplysninger(
            behandlingskjedeId = UUID.randomUUID(),
            opplysninger = Opplysninger.med(*opplysninger),
        )

    private fun harLøpendeRett(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate = LocalDate.MAX,
        verdi: Boolean = true,
    ) = Faktum(KravPåDagpenger.harLøpendeRett, verdi, Gyldighetsperiode(fraOgMed, tilOgMed))

    @Test
    fun `rettighetsperioder bygges fra harLøpendeRett-opplysninger`() {
        val service =
            PersonoppslagService(
                StubRepository(listOf(kjedeMed(harLøpendeRett(fraOgMed = mandag)))),
            )

        val perioder = service.hentRettighetsperioder("12345678910", mandag, tirsdag)

        perioder shouldHaveSize 1
        with(perioder.single()) {
            fraOgMed shouldBe mandag
            tilOgMed shouldBe null
            harRett shouldBe true
            ytelseType shouldBe YtelsestypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER
        }
    }

    @Test
    fun `perioder uten rett returneres med harRett false`() {
        val service =
            PersonoppslagService(
                StubRepository(
                    listOf(kjedeMed(harLøpendeRett(fraOgMed = mandag, tilOgMed = tirsdag, verdi = false))),
                ),
            )

        val perioder = service.hentRettighetsperioder("12345678910", mandag, tirsdag)

        perioder shouldHaveSize 1
        with(perioder.single()) {
            fraOgMed shouldBe mandag
            tilOgMed shouldBe tirsdag
            harRett shouldBe false
        }
    }

    @Test
    fun `perioder utenfor forespørselsperioden filtreres bort`() {
        val service =
            PersonoppslagService(
                StubRepository(
                    listOf(kjedeMed(harLøpendeRett(fraOgMed = mandag.minusMonths(2), tilOgMed = mandag.minusMonths(1)))),
                ),
            )

        service.hentRettighetsperioder("12345678910", mandag, tirsdag) shouldHaveSize 0
    }

    @Test
    fun `ytelsestype utledes fra rettighetstype-opplysning`() {
        val permittert = boolsk(OpplysningsTyper.PermittertId, "Bruker er permittert")
        val service =
            PersonoppslagService(
                StubRepository(
                    listOf(
                        kjedeMed(
                            harLøpendeRett(fraOgMed = mandag),
                            Faktum(permittert, true, Gyldighetsperiode(mandag, LocalDate.MAX)),
                        ),
                    ),
                ),
            )

        service.hentRettighetsperioder("12345678910", mandag, null).single().ytelseType shouldBe
            YtelsestypeDTO.DAGPENGER_PERMITTERING_ORDINAER
    }

    @Test
    fun `beregninger bygges fra utbetalingsopplysninger`() {
        val service =
            PersonoppslagService(
                StubRepository(
                    listOf(
                        kjedeMed(
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

        val beregninger = service.hentBeregninger("12345678910", mandag, tirsdag)

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
            PersonoppslagService(
                StubRepository(
                    listOf(
                        kjedeMed(
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

        service.hentBeregninger("12345678910", mandag, mandag).single().gjenståendeDager shouldBe 260
    }
}
