package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.november
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

internal class BetingedeAvhengigheterTest {
    private val start = 3.november(2026)
    private val sjekk = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Sjekk")
    private val sannVerdi = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "Sann verdi")
    private val usannVerdi = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "Usann verdi")
    private val resultat = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "Resultat")
    private val avhengigheter = Avhengigheter.Betinget(sjekk, listOf(sannVerdi), listOf(usannVerdi))
    private val regelsett =
        vilkår("Isolert grenvalg") {
            regel(sjekk) { innhentes }
            regel(sannVerdi) { innhentes }
            regel(usannVerdi) { innhentes }
            regel(resultat) { VelgVerdi(resultat, sjekk, sannVerdi, usannVerdi) }
        }

    @Test
    fun `uavklart grenvalg etterspør bare sjekken`() {
        val opplysninger = Opplysninger()

        avhengigheter.løs(opplysninger.forDato(start)) shouldBe LøsteAvhengigheter.Uavklart(listOf(sjekk))
        kjøring(opplysninger).evaluer().informasjonsbehov.keys shouldBe setOf(sjekk)
        opplysninger.forDato(start).har(resultat) shouldBe false
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `manglende valgt verdi etterspør bare valgt gren uten å bruke den andre som reserve`(verdi: Boolean) {
        val opplysninger = Opplysninger()
        val valgt = if (verdi) sannVerdi else usannVerdi
        val uvalgt = if (verdi) usannVerdi else sannVerdi
        opplysninger.leggTil(Faktum(sjekk, verdi, gyldighetsperiode = Gyldighetsperiode(start)))
        opplysninger.leggTil(Faktum(uvalgt, 999.0, gyldighetsperiode = Gyldighetsperiode(start)))

        kjøring(opplysninger).evaluer().informasjonsbehov.keys shouldBe setOf(valgt)
        opplysninger.forDato(start).har(resultat) shouldBe false
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `beregner resultat selv om uvalgt gren mangler`(verdi: Boolean) {
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(sjekk, verdi, gyldighetsperiode = Gyldighetsperiode(start)))
        opplysninger.leggTil(Faktum(if (verdi) sannVerdi else usannVerdi, 100.0, gyldighetsperiode = Gyldighetsperiode(start)))

        kjøring(opplysninger).evaluer().informasjonsbehov.keys shouldBe emptySet()
        opplysninger.forDato(start).finnOpplysning(resultat).verdi shouldBe 100.0
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `resultatet bruker bare sjekken og valgt gren som grunnlag`(verdi: Boolean) {
        val opplysninger = grunnlag(verdi)

        kjøring(opplysninger).evaluer().informasjonsbehov.keys shouldBe emptySet()

        val produkt = opplysninger.forDato(start).finnOpplysning(resultat)
        produkt.verdi shouldBe if (verdi) 100.0 else 50.0
        requireNotNull(produkt.utledetAv).opplysninger.map { it.opplysningstype }.toSet() shouldBe
            setOf(sjekk, if (verdi) sannVerdi else usannVerdi)
    }

    @Test
    fun `endring i uvalgt gren beregner ikke resultatet på nytt`() {
        val opplysninger = grunnlag(true)
        kjøring(opplysninger).evaluer()
        val før = opplysninger.forDato(start).finnOpplysning(resultat)

        opplysninger.leggTil(Faktum(usannVerdi, 75.0, gyldighetsperiode = Gyldighetsperiode(start)))

        kjøring(opplysninger).evaluer().kjørteRegler.size shouldBe 0
        opplysninger.forDato(start).finnOpplysning(resultat).id shouldBe før.id
    }

    @Test
    fun `samtidig grenbytte og endring av valgt verdi gir gjeldende resultat uten løkke`() {
        val opplysninger = grunnlag(true)
        kjøring(opplysninger).evaluer()
        val før = opplysninger.forDato(start).finnOpplysning(resultat)

        opplysninger.leggTil {
            it.add(Faktum(sjekk, false, gyldighetsperiode = Gyldighetsperiode(start)))
            it.add(Faktum(usannVerdi, 75.0, gyldighetsperiode = Gyldighetsperiode(start)))
        }
        kjøring(opplysninger).evaluer().informasjonsbehov.keys shouldBe emptySet()

        val etter = opplysninger.forDato(start).finnOpplysning(resultat)
        etter.verdi shouldBe 75.0
        (etter.id == før.id) shouldBe false
        requireNotNull(etter.utledetAv).opplysninger.map { it.opplysningstype }.toSet() shouldBe setOf(sjekk, usannVerdi)
    }

    @Test
    fun `utdatert sjekk beregnes før motoren etterspør en gren`() {
        val kilde = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "Grunnlag for sjekk")
        val medUtledetSjekk =
            vilkår("Utledet grenvalg") {
                regel(kilde) { innhentes }
                regel(sjekk) { ikke(kilde) }
                regel(sannVerdi) { innhentes }
                regel(usannVerdi) { innhentes }
                regel(resultat) { VelgVerdi(resultat, sjekk, sannVerdi, usannVerdi) }
            }
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(kilde, true, gyldighetsperiode = Gyldighetsperiode(start)))
        opplysninger.leggTil(Faktum(usannVerdi, 50.0, gyldighetsperiode = Gyldighetsperiode(start)))
        kjøring(opplysninger, regler = medUtledetSjekk).evaluer()
        opplysninger.forDato(start).finnOpplysning(resultat).verdi shouldBe 50.0

        opplysninger.leggTil(Faktum(kilde, false, gyldighetsperiode = Gyldighetsperiode(start)))
        opplysninger.fjern(usannVerdi)

        kjøring(opplysninger, regler = medUtledetSjekk).evaluer().informasjonsbehov.keys shouldBe setOf(sannVerdi)
        opplysninger.forDato(start).finnOpplysning(sjekk).verdi shouldBe true

        opplysninger.leggTil(Faktum(sannVerdi, 100.0, gyldighetsperiode = Gyldighetsperiode(start)))
        kjøring(opplysninger, regler = medUtledetSjekk).evaluer().informasjonsbehov.keys shouldBe emptySet()
        opplysninger.forDato(start).finnOpplysning(resultat).verdi shouldBe 100.0
    }

    @Test
    fun `tilbakevirkende grenbytte erstatter arvet resultat bare i berørte perioder`() {
        val tidligere = grunnlag(true)
        val senere = 10.november(2026)
        kjøring(tidligere, datoer = listOf(start, senere)).evaluer()
        val arvetResultat = tidligere.forDato(senere).finnOpplysning(resultat)
        val opplysninger = Opplysninger.basertPå(tidligere)

        opplysninger.leggTil(Faktum(sjekk, false, gyldighetsperiode = Gyldighetsperiode(5.november(2026))))
        kjøring(opplysninger, datoer = listOf(start, 5.november(2026), senere)).evaluer().informasjonsbehov.keys shouldBe emptySet()

        opplysninger.forDato(start).finnOpplysning(resultat).id shouldBe arvetResultat.id
        for (dato in listOf(5.november(2026), senere)) {
            val gjeldende = opplysninger.forDato(dato).finnOpplysning(resultat)
            gjeldende.verdi shouldBe 50.0
            (gjeldende.id == arvetResultat.id) shouldBe false
            requireNotNull(gjeldende.utledetAv).opplysninger.map { it.opplysningstype }.toSet() shouldBe setOf(sjekk, usannVerdi)
        }
        tidligere.forDato(senere).finnOpplysning(resultat).verdi shouldBe 100.0
    }

    private fun grunnlag(verdi: Boolean) =
        Opplysninger().apply {
            leggTil(Faktum(sjekk, verdi, gyldighetsperiode = Gyldighetsperiode(start)))
            leggTil(Faktum(sannVerdi, 100.0, gyldighetsperiode = Gyldighetsperiode(start)))
            leggTil(Faktum(usannVerdi, 50.0, gyldighetsperiode = Gyldighetsperiode(start)))
        }

    private fun kjøring(
        opplysninger: Opplysninger,
        datoer: List<LocalDate> = listOf(start),
        regler: Regelsett = regelsett,
    ): Regelkjøring {
        // Testkonstruktøren til Regelkjøring ønsker alle outputs, også de uvalgte grenene.
        val prosess =
            object : Forretningsprosess(Regelverk(RegelverkType("Betingede avhengigheter"), regelsett = arrayOf(regler))) {
                override fun ønsketResultat(opplysninger: LesbarOpplysninger) = setOf(resultat)

                override fun virkningsdato(opplysninger: LesbarOpplysninger) = start

                override fun regelkjøring(opplysninger: Opplysninger) = kjøring(opplysninger, datoer, regler)
            }
        return Regelkjøring(start, datoer, opplysninger, prosess)
    }

    private class VelgVerdi(
        resultat: Opplysningstype<Double>,
        private val sjekk: Opplysningstype<Boolean>,
        private val hvisSann: Opplysningstype<Double>,
        private val hvisUsann: Opplysningstype<Double>,
    ) : Regel<Double>(
            resultat,
            avhengigheter = Avhengigheter.Betinget(sjekk, listOf(hvisSann), listOf(hvisUsann)),
        ) {
        override fun kjør(
            opplysninger: LesbarOpplysninger,
            prøvingsdato: LocalDate,
        ): Double = opplysninger.finnOpplysning(if (opplysninger.finnOpplysning(sjekk).verdi) hvisSann else hvisUsann).verdi

        override fun toString() = "Isolert testregel med betingede avhengigheter"
    }
}
