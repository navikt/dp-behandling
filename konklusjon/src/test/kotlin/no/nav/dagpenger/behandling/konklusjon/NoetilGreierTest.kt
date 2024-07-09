package no.nav.dagpenger.behandling.konklusjon

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.konklusjon.SammensattKonklusjon.Companion.alle
import no.nav.dagpenger.behandling.konklusjon.SammensattKonklusjon.Companion.enAv
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import org.junit.jupiter.api.Test
import java.time.LocalDate

class NoetilGreierTest {
    private companion object {
        val opplysningstypeA = Opplysningstype.somBoolsk("Test1")
        val opplysningstypeB = Opplysningstype.somBoolsk("Test2")
    }

    private val opplysninger =
        Opplysninger().apply {
            registrer(Regelkjøring(LocalDate.now(), this))
        }

    private val avslagPåAlder = Konklusjonspunkt { Konklusjonsresultat(it.har(opplysningstypeA), "Blurp") }
    private val avslagPåInntekt = Konklusjonspunkt { Konklusjonsresultat(it.har(opplysningstypeB), "Blarp") }

    private val enAv = enAv(avslagPåAlder, avslagPåInntekt)
    private val alle = alle(avslagPåAlder, avslagPåInntekt)

    @Test
    fun `gjør ingen konklusjon`() {
        enAv.kanKonkludere(opplysninger).kanKonkludere shouldBe false
        alle.kanKonkludere(opplysninger).kanKonkludere shouldBe false
    }

    @Test
    fun `konkluderer så snart vi kan med enAv`() {
        opplysninger.leggTil(Faktum(opplysningstypeA, true))

        enAv.kanKonkludere(opplysninger).kanKonkludere shouldBe true
        alle.kanKonkludere(opplysninger).kanKonkludere shouldBe false
    }

    @Test
    fun `konkluderer så snart vi kan med alle`() {
        opplysninger.leggTil(Faktum(opplysningstypeA, true))
        opplysninger.leggTil(Faktum(opplysningstypeB, true))

        enAv.kanKonkludere(opplysninger).kanKonkludere shouldBe true
        alle.kanKonkludere(opplysninger).kanKonkludere shouldBe true
    }

    @Test
    fun `støtter kombinasjon av kombinasjoner`() {
        val konklusjon = alle(enAv(alle(alle, enAv), alle(enAv)), enAv(alle))
        opplysninger.leggTil(Faktum(opplysningstypeA, true))
        opplysninger.leggTil(Faktum(opplysningstypeB, true))

        konklusjon.kanKonkludere(opplysninger).kanKonkludere shouldBe true
        konklusjon.kanKonkludere(opplysninger).årsak shouldBe
            "alle(enAv(alle(alle(Blurp, Blarp), enAv(Blurp, Blarp)), alle(enAv(Blurp, Blarp))), enAv(alle(Blurp, Blarp)))"
    }

    @Test
    fun `tar med årsak`() {
        val konklusjon = enAv(avslagPåAlder, avslagPåInntekt)
        opplysninger.leggTil(Faktum(opplysningstypeA, true))

        konklusjon.kanKonkludere(opplysninger) shouldBe Konklusjonsresultat(true, "enAv(Blurp, Blarp)")
    }
}

private data class Konklusjonsresultat(
    val kanKonkludere: Boolean,
    val årsak: String,
)

private fun interface Konklusjonspunkt {
    fun kanKonkludere(opplysninger: LesbarOpplysninger): Konklusjonsresultat
}

private abstract class SammensattKonklusjon(
    private vararg val konklusjoner: Konklusjonspunkt,
) : Konklusjonspunkt {
    companion object {
        fun enAv(vararg konklusjon: Konklusjonspunkt): Konklusjonspunkt = EnAvKonklusjon(*konklusjon)

        fun alle(vararg konklusjon: Konklusjonspunkt): Konklusjonspunkt = AlleKonklusjon(*konklusjon)
    }

    protected abstract val navn: String

    protected abstract fun harKonklusjon(resultat: List<Konklusjonsresultat>): Boolean

    override fun kanKonkludere(opplysninger: LesbarOpplysninger): Konklusjonsresultat {
        val resultat = konklusjoner.map { it.kanKonkludere(opplysninger) }
        val result = harKonklusjon(resultat)
        val cause = "$navn(${resultat.joinToString { it.årsak }})"
        return Konklusjonsresultat(result, cause)
    }
}

private class EnAvKonklusjon(
    vararg konklusjon: Konklusjonspunkt,
) : SammensattKonklusjon(*konklusjon) {
    override fun harKonklusjon(resultat: List<Konklusjonsresultat>) = resultat.any { it.kanKonkludere }

    override val navn: String
        get() = "enAv"
}

private class AlleKonklusjon(
    vararg konklusjon: Konklusjonspunkt,
) : SammensattKonklusjon(*konklusjon) {
    override fun harKonklusjon(delkonklusjoner: List<Konklusjonsresultat>) = delkonklusjoner.all { it.kanKonkludere }

    override val navn: String
        get() = "alle"
}
