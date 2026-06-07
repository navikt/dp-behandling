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

    @Test
    fun `rotregelen er blokkert`() {
        val opplysninger = Opplysninger()
        val plan = regeltre.lagPlan(opplysninger, setOf(antallFruktRegel))
        plan shouldBe emptySet()
    }

    @Test
    fun `avhengighet er blokkert`() {
        val opplysninger = Opplysninger()
        val plan = regeltre.lagPlan(opplysninger, setOf(antallEplerRegel))
        plan shouldBe emptySet()
    }

    @Test
    fun `ekstern regel hindrer at de som er avhengig av den kan kjøre`() {
        val antallEpletrærType =
            Opplysningstype.heltall(
                id = Opplysningstype.Id(UUIDv7.ny(), Heltall),
                beskrivelse = "Antall epletrær",
            )
        val antallEplerPerTreType =
            Opplysningstype.heltall(
                id = Opplysningstype.Id(UUIDv7.ny(), Heltall),
                beskrivelse = "Antall epler per tre",
            )

        val antallEplerPerTre = 1
        val antallEpletrærRegel = Ekstern(antallEpletrærType, listOf())
        val antallEplerPerTreRegel = Utgangspunkt(antallEplerPerTreType, antallEplerPerTre)
        val antallEplerRegel = Multiplikasjon(antallEplerType, antallEpletrærType, antallEplerPerTreType, Int::times)

        val opplysningstypeTilRegel: Map<Opplysningstype<*>, Regel<*>> =
            mapOf(
                antallEpletrærType to antallEpletrærRegel,
                antallEplerPerTreType to antallEplerPerTreRegel,
                antallEplerType to antallEplerRegel,
                antallBananerType to antallBananerRegel,
                antallFruktType to antallFruktRegel,
            )

        val regeltre = antallFruktRegel.regeltre(opplysningstypeTilRegel)

        val opplysninger = Opplysninger()
        val plan = regeltre.lagPlan(opplysninger)
        plan shouldBe setOf(antallEplerPerTreRegel, antallBananerRegel)
    }

    private fun TreNode<Regel<*>>.lagPlan(
        opplysninger: Opplysninger,
        blokkerteRegler: Collection<Regel<*>> = emptyList(),
    ): Set<Regel<*>> =
        this
            .somRegelnode()
            .flaggReglerSomMåKjøres(opplysninger)
            .flaggReglerSomErBlokkert(blokkerteRegler)
            // trenger kun kjøre hele treet hvis roten må kjøre, ellers er det ikke vits
            .takeIf { it.verdi.kanKjøre }
            ?.topologisk()
            // kun de som må kjøres, er ikke gitt at hele treet skal kjøres
            ?.filter { it.verdi.kanKjøringGjennomføres }
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

    private fun TreNode<Regelnode>.flaggReglerSomErBlokkert(blokkerteRegler: Collection<Regel<*>>): TreNode<Regelnode> {
        fun TreNode<Regelnode>.erBlokkert(): Boolean {
            if (this.verdi.erBlokkert) return true
            return this.avhengigheter.any { it.erBlokkert() }
        }

        val avhengigheter = avhengigheter.map { it.flaggReglerSomErBlokkert(blokkerteRegler) }
        val harBlokkertAvhengighet = avhengigheter.any { it.erBlokkert() }
        return copy(
            verdi = verdi.copy(erBlokkert = blokkerteRegler.contains(verdi.regel) || harBlokkertAvhengighet),
            avhengigheter = avhengigheter,
        )
    }

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
        val avhengighetAvventerData = avhengigheter.any { it.verdi.avventerData }

        val harFåttNyeAvhengigheterIKode =
            opplysningerUtledetAv != null &&
                this.verdi.regel.avhengerAv
                    .toSet() != opplysningerUtledetAv.map { it.opplysningstype }.toSet()

        val kjøreflagg =
            when {
                produkt == null -> Regelnode.Kjøreflagg.MANGLER_PRODUKT
                harUtdaterteAvhengigheter -> Regelnode.Kjøreflagg.HAR_UTDATERT_AVHENGIGHET
                avhengighetSkalKjøre -> Regelnode.Kjøreflagg.AVHENGIGHET_MÅ_KJØRE
                harFåttNyeAvhengigheterIKode -> Regelnode.Kjøreflagg.HAR_FÅTT_ENDRET_AVHENGIGHETER_I_KODE
                else -> Regelnode.Kjøreflagg.INGEN_KJØRING_NØDVENDIG
            }
        return copy(
            verdi =
                verdi.copy(
                    avventerData = this.verdi.regel is Ekstern || avhengighetAvventerData,
                    kjøreflagg = kjøreflagg,
                ),
            avhengigheter = avhengigheter,
        )
    }

    private data class Regelnode(
        val regel: Regel<*>,
        val erBlokkert: Boolean = false,
        val avventerData: Boolean = false,
        val kjøreflagg: Kjøreflagg = Kjøreflagg.INGEN_KJØRING_NØDVENDIG,
    ) {
        val kanKjøre = kjøreflagg.måKjøres() && !erBlokkert
        val kanKjøringGjennomføres = kanKjøre && !avventerData

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
