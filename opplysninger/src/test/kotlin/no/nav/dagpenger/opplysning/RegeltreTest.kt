package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.regel.Utgangspunkt
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class RegeltreTest {
    private val antallFruktType =
        Opplysningstype.heltall(
            id = Opplysningstype.Id(UUIDv7.ny(), Heltall),
            beskrivelse = "Summen av antall epler og antall bananer",
        )
    private val antallEplerType =
        Opplysningstype.heltall(
            id = Opplysningstype.Id(UUIDv7.ny(), Heltall),
            beskrivelse = "Antall epler",
        )
    private val antallBananerType =
        Opplysningstype.heltall(
            id = Opplysningstype.Id(UUIDv7.ny(), Heltall),
            beskrivelse = "Antall bananer",
        )

    private val antallEpler = 10
    private val antallBananer = 22

    private val antallFruktRegel = AddereHeltall(antallFruktType, listOf(antallEplerType, antallBananerType))
    private val antallEplerRegel = Utgangspunkt(antallEplerType, antallEpler)
    private val antallBananerRegel = Utgangspunkt(antallBananerType, antallBananer)

    private val opplysningstypeTilRegel: Map<Opplysningstype<*>, Regel<*>> =
        mapOf(
            antallEplerType to antallEplerRegel,
            antallBananerType to antallBananerRegel,
            antallFruktType to antallFruktRegel,
        )

    private val regeltre = antallFruktRegel.regeltre(opplysningstypeTilRegel)

    @Test
    fun `sjekke at treet er riktig`() {
        regeltre shouldBe
            TreNode(
                verdi = antallFruktRegel,
                avhengigheter =
                    listOf(
                        TreNode(antallEplerRegel),
                        TreNode(antallBananerRegel),
                    ),
            )
    }

    @Test
    fun `hvis ingen regler har produsert resultat må alle regler må kjøre`() {
        val opplysninger = Opplysninger()
        val plan = regeltre.lagPlan(opplysninger)
        plan shouldBe setOf(antallEplerRegel, antallBananerRegel, antallFruktRegel)
    }

    private fun TreNode<Regel<*>>.lagPlan(opplysninger: Opplysninger): Set<Regel<*>> = this.topologisk().map { it.verdi }.toSet()

    class AddereHeltall(
        produserer: Opplysningstype<Int>,
        private val ledd: List<Opplysningstype<Int>>,
    ) : Regel<Int>(produserer, ledd) {
        override fun kjør(opplysninger: LesbarOpplysninger): Int {
            val verdier = ledd.map { opplysninger.finnOpplysning(it).verdi }
            return verdier.sumOf { it }
        }

        override fun toString(): String = "Beregner $produserer ved å legge sammen ${ledd.joinToString(" + ")}"
    }
}
