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

    @Test
    fun `hvis noen regler har produsert resultat må de uten produkt kjøre`() {
        val opplysninger = Opplysninger.med(Faktum(antallEplerType, antallEpler))
        val plan = regeltre.lagPlan(opplysninger)
        plan shouldBe setOf(antallBananerRegel, antallFruktRegel)
    }

    @Test
    fun `begge avhengigheter har produsert resultat`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(antallEplerType, antallEpler),
                Faktum(antallBananerType, antallBananer),
            )
        val plan = regeltre.lagPlan(opplysninger)
        plan shouldBe setOf(antallFruktRegel)
    }

    @Test
    fun `en avhengighet mangler resultat`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(antallBananerType, antallBananer),
                Faktum(antallFruktType, antallEpler + antallBananer),
            )
        val plan = regeltre.lagPlan(opplysninger)
        plan shouldBe setOf(antallEplerRegel, antallFruktRegel)
    }

    private fun TreNode<Regel<*>>.lagPlan(opplysninger: Opplysninger): Set<Regel<*>> =
        this
            .somRegelnode()
            .flaggReglerSomMåKjøres(opplysninger)
            // trenger kun kjøre hele treeet hvis roten må kjøre, ellers er det ikke vits
            .takeIf { it.verdi.kjøreflagg.måKjøres() }
            ?.topologisk()
            // kun de som må kjøres, er ikke gitt at hele treet skal kjøres
            ?.filter { it.verdi.kjøreflagg.måKjøres() }
            ?.map { it.verdi.regel }
            ?.toSet()
            ?: emptySet()

    private fun TreNode<Regel<*>>.somRegelnode(): TreNode<Regelnode> =
        TreNode(
            Regelnode(verdi),
            avhengigheter =
                avhengigheter.map {
                    it.somRegelnode()
                },
        )

    private fun TreNode<Regelnode>.flaggReglerSomMåKjøres(opplysninger: Opplysninger): TreNode<Regelnode> {
        val avhengigheter = avhengigheter.map { it.flaggReglerSomMåKjøres(opplysninger) }

        val produkt = opplysninger.finnNullableOpplysning(verdi.regel.produserer)
        val opplysningerUtledetAv = produkt?.utledetAv?.opplysninger
        // sjekker ikke om regelen selv sin opplysning er utdatert 🤔
        val harUtdaterteAvhengigheter = opplysningerUtledetAv?.any { it.erUtdatert } == true

        fun TreNode<Regelnode>.måKjøre(): Boolean {
            if (this.verdi.kjøreflagg.måKjøres()) return true
            return this.avhengigheter.any { it.måKjøre() }
        }

        // hvis en avhengighet tidligere i kjeden er planlagt skal vi også kjøre
        val avhengighetSkalKjøre = avhengigheter.any { it.måKjøre() }

        val harFåttNyeAvhengigheterIKode =
            opplysningerUtledetAv != null &&
                this.verdi.regel.avhengerAv
                    .toSet() != opplysningerUtledetAv.map { it.opplysningstype }.toSet()

        return copy(
            verdi =
                verdi.copy(
                    kjøreflagg =
                        when {
                            produkt == null -> Regelnode.Kjøreflagg.MANGLER_PRODUKT
                            harUtdaterteAvhengigheter -> Regelnode.Kjøreflagg.HAR_UTDATERT_AVHENGIGHET
                            avhengighetSkalKjøre -> Regelnode.Kjøreflagg.AVHENGIGHET_MÅ_KJØRE
                            harFåttNyeAvhengigheterIKode -> Regelnode.Kjøreflagg.HAR_FÅTT_ENDRET_AVHENGIGHETER_I_KODE
                            else -> Regelnode.Kjøreflagg.INGEN_KJØRING_NØDVENDIG
                        },
                ),
            avhengigheter = avhengigheter,
        )
    }

    private data class Regelnode(
        val regel: Regel<*>,
        val erBlokkert: Boolean = false,
        val kjøreflagg: Kjøreflagg = Kjøreflagg.INGEN_KJØRING_NØDVENDIG,
    ) {
        enum class Kjøreflagg {
            INGEN_KJØRING_NØDVENDIG,
            MANGLER_PRODUKT,
            HAR_UTDATERT_AVHENGIGHET,
            AVHENGIGHET_MÅ_KJØRE,
            HAR_FÅTT_ENDRET_AVHENGIGHETER_I_KODE,
            ;

            fun måKjøres() =
                when (this) {
                    INGEN_KJØRING_NØDVENDIG -> false

                    MANGLER_PRODUKT,
                    HAR_UTDATERT_AVHENGIGHET,
                    AVHENGIGHET_MÅ_KJØRE,
                    HAR_FÅTT_ENDRET_AVHENGIGHETER_I_KODE,
                    -> true
                }
        }
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
