package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Multiplikasjon
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

    private val antallEpletrærType =
        Opplysningstype.heltall(
            id = Opplysningstype.Id(UUIDv7.ny(), Heltall),
            beskrivelse = "Antall epletrær",
        )
    private val antallEplerPerTreType =
        Opplysningstype.heltall(
            id = Opplysningstype.Id(UUIDv7.ny(), Heltall),
            beskrivelse = "Antall epler per tre",
        )

    private val antallEplerPerTre = 1
    private val antallEpletrær = 4
    private val antallEpler = 10
    private val antallBananer = 22

    private val antallEpletrærRegel = Ekstern(antallEpletrærType, listOf())
    private val antallEplerPerTreRegel = Utgangspunkt(antallEplerPerTreType, antallEplerPerTre)
    private val antallEplerMedEksternAvhengighetRegel =
        Multiplikasjon(antallEplerType, antallEpletrærType, antallEplerPerTreType, Int::times)

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

    val regeltreMedEksternRegel =
        antallFruktRegel.regeltre(
            mapOf(
                antallEpletrærType to antallEpletrærRegel,
                antallEplerPerTreType to antallEplerPerTreRegel,
                antallEplerType to antallEplerMedEksternAvhengighetRegel,
                antallBananerType to antallBananerRegel,
                antallFruktType to antallFruktRegel,
            ),
        )

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
        plan shouldBe Kjøreplanresultat(setOf(antallEplerRegel, antallBananerRegel, antallFruktRegel), emptySet())
    }

    @Test
    fun `hvis noen regler har produsert resultat må de uten produkt kjøre`() {
        val opplysninger = Opplysninger.med(Faktum(antallEplerType, antallEpler))
        val plan = regeltre.lagPlan(opplysninger)
        plan shouldBe Kjøreplanresultat(setOf(antallBananerRegel, antallFruktRegel), emptySet())
    }

    @Test
    fun `begge avhengigheter har produsert resultat`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(antallEplerType, antallEpler),
                Faktum(antallBananerType, antallBananer),
            )
        val plan = regeltre.lagPlan(opplysninger)
        plan shouldBe Kjøreplanresultat(setOf(antallFruktRegel), emptySet())
    }

    @Test
    fun `en avhengighet mangler resultat`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(antallBananerType, antallBananer),
                Faktum(antallFruktType, antallEpler + antallBananer),
            )
        val plan = regeltre.lagPlan(opplysninger)
        plan shouldBe Kjøreplanresultat(setOf(antallEplerRegel, antallFruktRegel), emptySet())
    }

    @Test
    fun `rotregelen er blokkert`() {
        val opplysninger = Opplysninger()
        val plan = regeltre.lagPlan(opplysninger, setOf(antallFruktRegel))
        plan shouldBe Kjøreplanresultat(emptySet(), emptySet())
    }

    @Test
    fun `avhengighet er blokkert`() {
        val opplysninger = Opplysninger()
        val plan = regeltre.lagPlan(opplysninger, setOf(antallEplerRegel))
        plan shouldBe Kjøreplanresultat(emptySet(), emptySet())
    }

    @Test
    fun `ekstern regel hindrer at de som er avhengig av den kan kjøre`() {
        val opplysninger = Opplysninger()
        val plan = regeltreMedEksternRegel.lagPlan(opplysninger)
        plan shouldBe
            Kjøreplanresultat(
                setOf(antallEplerPerTreRegel, antallBananerRegel),
                setOf(antallEpletrærRegel, antallEplerMedEksternAvhengighetRegel, antallFruktRegel),
            )
    }

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
