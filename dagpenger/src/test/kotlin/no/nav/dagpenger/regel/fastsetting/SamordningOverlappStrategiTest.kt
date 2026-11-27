package no.nav.dagpenger.regel.fastsetting

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Opplysninger i § 4-26 og § 4-12 som er avledet av [Samordning.samordnetDagsats] (BEVAR_HØYRE)
 * må få samme periodisering som kilden.
 */
class SamordningOverlappStrategiTest {
    private val fraDato = 10.mai(2024)
    private val sykepengerTilOgMed = fraDato.plusDays(10)

    private val opplysninger = Opplysninger()
    private val regelsett = RegelverkDagpenger.regelsettFor(DagpengenesStørrelse.ukessats)

    private fun evaluerDag(dato: LocalDate) = Regelkjøring(dato, opplysninger, *regelsett.toTypedArray()).evaluer()

    private val kilde = Systemkilde(UUIDv7.ny(), LocalDateTime.now())

    private fun <T : Any> faktum(
        type: Opplysningstype<T>,
        verdi: T,
        periode: Gyldighetsperiode = Gyldighetsperiode(),
    ) = Faktum(type, verdi, periode, kilde = kilde)

    private fun evaluerAlleDager() = (0..15L).forEach { evaluerDag(fraDato.plusDays(it)) }

    init {
        val sykepengerPeriode = Gyldighetsperiode(fraDato, sykepengerTilOgMed)
        listOf(
            faktum(Søknadstidspunkt.søknadsdato, fraDato),
            faktum(Søknadstidspunkt.ønsketdato, fraDato),
            faktum(DagpengenesStørrelse.barn, BarnListe(UUIDv7.ny(), emptyList())),
            faktum(Samordning.sykepenger, false),
            faktum(Samordning.pleiepenger, false),
            faktum(Samordning.omsorgspenger, false),
            faktum(Samordning.opplæringspenger, false),
            faktum(Samordning.uføre, false),
            faktum(Samordning.foreldrepenger, false),
            faktum(Samordning.svangerskapspenger, false),
            faktum(Dagpengegrunnlag.grunnlag, Beløp(500_000)),
        ).forEach { opplysninger.leggTil(it as Opplysning<*>) }
        evaluerAlleDager()
        listOf(
            faktum(Samordning.sykepengerDagsats, Beløp(200), sykepengerPeriode),
            faktum(Samordning.sykepenger, true, sykepengerPeriode),
        ).forEach { opplysninger.leggTil(it as Opplysning<*>) }
        evaluerAlleDager()
    }

    private fun perioder(type: Opplysningstype<*>) = opplysninger.finnAlle(type).map { it.gyldighetsperiode }.sortedBy { it.fraOgMed }

    @Test
    fun `samordnet dagsats deles opp i perioder med og uten sykepenger`() {
        perioder(Samordning.samordnetDagsats).size shouldBe 2
    }

    @Test
    fun `opplysninger avledet av samordnet dagsats har samme periodisering som kilden`() {
        val forventet = perioder(Samordning.samordnetDagsats)

        listOf(
            SamordingUtenforFolketrygden.samordnetUkessatsUtenBarnetillegg,
            SamordingUtenforFolketrygden.samordnetUkessats,
            SamordingUtenforFolketrygden.dagsatsSamordnetUtenforFolketrygden,
            DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg,
            DagpengenesStørrelse.ukessats,
            DagpengenesStørrelse.harSamordnet,
        ).forEach { type ->
            withClue(type.navn) { perioder(type) shouldBe forventet }
        }
    }

    @Test
    fun `korreksjon midt i sykepengeperioden beholder dagsatsen til høyre like mye som kilden`() {
        val midt = Gyldighetsperiode(fraDato.plusDays(5), fraDato.plusDays(7))
        opplysninger.leggTil(faktum(Samordning.sykepengerDagsats, Beløp(300), midt) as Opplysning<*>)
        opplysninger.leggTil(faktum(Samordning.sykepenger, true, midt) as Opplysning<*>)
        evaluerAlleDager()

        val dagEtterKorreksjon = fraDato.plusDays(8)
        val forDato = opplysninger.forDato(dagEtterKorreksjon)
        withClue("sykepenger (BEVAR_HØYRE) gjelder fortsatt $dagEtterKorreksjon") {
            forDato.finnOpplysning(Samordning.sykepenger).verdi shouldBe true
        }
        withClue("sykepengerDagsats (ERSTATT_FRA_FOM) mister høyre del og blir ikke 200 $dagEtterKorreksjon") {
            forDato.finnOpplysning(Samordning.sykepengerDagsats).verdi shouldBe Beløp(200)
        }
    }
}
