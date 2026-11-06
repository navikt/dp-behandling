package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelplanlegger
import no.nav.dagpenger.opplysning.Utledning
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class RegelStatusTest {
    private val a = type("A")
    private val b = type("B")
    private val regelA = Ekstern(a, emptyList())
    private val regelB = MinstAv(b, a)
    private val produsenter: Map<Opplysningstype<out Any>, Regel<*>> = listOf<Regel<*>>(regelA, regelB).associateBy { it.produserer }

    private fun planlegg(opplysninger: Opplysninger) =
        Regelplanlegger().also { regelB.lagPlan(opplysninger, it, produsenter, mutableSetOf()) }.status

    @Test
    fun `regel som venter på ekstern opplysning er blokkert med hva den venter på`() {
        val status = planlegg(Opplysninger())

        status[regelA] shouldBe RegelStatus.Kjørbar
        status[regelB] shouldBe RegelStatus.Blokkert(setOf(a))
    }

    @Test
    fun `opplysning uten utledning er overstyrt og følges ikke videre`() {
        val status = planlegg(Opplysninger.med(Faktum(a, 1.0), Faktum(b, 1.0)))

        status[regelB] shouldBe RegelStatus.Overstyrt
        status[regelA] shouldBe null
    }

    @Test
    fun `utledet opplysning med uendret grunnlag er gyldig`() {
        val faktumA = Faktum(a, 1.0)
        val status = planlegg(Opplysninger.med(faktumA, Faktum(b, 1.0, utledetAv = Utledning(regelB, listOf(faktumA)))))

        status[regelB] shouldBe RegelStatus.Gyldig
    }

    private fun type(navn: String) = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), navn)
}
