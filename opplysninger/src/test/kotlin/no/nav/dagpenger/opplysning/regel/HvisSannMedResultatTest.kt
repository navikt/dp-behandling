package no.nav.dagpenger.opplysning.regel

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.mai
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class HvisSannMedResultatTest {
    private val hvisSannVerdi = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "HvisSannVerdi")
    private val hvisUsannVerdi = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "HvisUsannVerdi")
    private val resultat = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "Resultat")

    private val regelsett =
        vilkår("Test") {
            regel(a) { innhentes }
            regel(hvisSannVerdi) { innhentes }
            regel(hvisUsannVerdi) { innhentes }
            regel(resultat) { hvisSannMedResultat(a, hvisSannVerdi, hvisUsannVerdi) }
        }

    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeEach
    fun setup() {
        regelkjøring = Regelkjøring(23.mai(2024), opplysninger = opplysninger, regelsett)
    }

    @Test
    fun `returnerer hvisSann-verdi når sjekk er true`() {
        opplysninger.leggTil(Faktum(a, true))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 100.0
    }

    @Test
    fun `returnerer hvisUsann-verdi når sjekk er false`() {
        opplysninger.leggTil(Faktum(a, false))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 50.0
    }

    @Test
    fun `re-evaluerer når hvisSann-verdi endres`() {
        opplysninger.leggTil(Faktum(a, true))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 100.0

        // hvisSann endrer seg – resultat skal oppdateres
        opplysninger.leggTil(Faktum(hvisSannVerdi, 200.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 200.0
    }

    @Test
    fun `re-evaluerer når hvisUsann-verdi endres`() {
        opplysninger.leggTil(Faktum(a, false))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 50.0

        // hvisUsann endrer seg – resultat skal oppdateres
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 75.0))
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(resultat).verdi shouldBe 75.0
    }

    @Test
    fun `re-evaluerer ikke i loop når verdier er uendret`() {
        opplysninger.leggTil(Faktum(a, true))
        opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
        opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
        val rapport = regelkjøring.evaluer()

        // Ingen regler skal kjøre på nytt – alt er stabilt
        rapport.kjørteRegler.size shouldBe 1
        opplysninger.finnOpplysning(resultat).verdi shouldBe 100.0
    }

    @Test
    fun `re-evaluerer korrekt ved omgjøring når hvisSann er endret til ny mellomberegning (reproduserer erErstattet-bug)`() {
        // Simluerer et scenario der koden endres mellom to evalueringer:
        // Gammelt regelsett: resultat = hvisSannMedResultat(a, direkteBeløp, nullBeløp)
        // Nytt regelsett:    resultat = hvisSannMedResultat(a, avrundtBeløp, nullBeløp)
        //                    der avrundtBeløp er en ny mellomberegning (avrund) av direkteBeløp
        //
        // Uten fiksen: når direkteBeløp erstattes i omgjøring, legger Regel.lagPlanFraUtledning
        // HvisSannMedResultat direkte i planen (erErstattet-caset) uten å sjekke at
        // avrundtBeløp (ny hvisSann) finnes — og kjør() kaster IllegalStateException.

        val direkteBeløp = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "DirekteBelop")
        val nullBeløp = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "NullBelop")
        val beløpResultat = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "BelopResultat")

        // Gammelt regelsett: beløpResultat bruker direkteBeløp som hvisSann
        val gammeltRegelsett =
            vilkår("TestGammelt") {
                regel(a) { innhentes }
                regel(direkteBeløp) { innhentes }
                regel(nullBeløp) { somUtgangspunkt(Beløp(0)) }
                regel(beløpResultat) { hvisSannMedResultat(a, direkteBeløp, nullBeløp) }
            }

        val opplysninger = Opplysninger()
        val gammelRegelkjøring = Regelkjøring(23.mai(2024), opplysninger, gammeltRegelsett)
        opplysninger.leggTil(Faktum(a, true))
        // DENNE BLIR MED OM DEN FINNES; IKKE OM DEN IKKE FINNES. Det burde nok vært determnisitisk
        // opplysninger.leggTil(Faktum(nullBeløp, Beløp(0)))
        opplysninger.leggTil(Faktum(direkteBeløp, Beløp(100)))
        gammelRegelkjøring.evaluer()
        opplysninger.finnOpplysning(beløpResultat).verdi shouldBe Beløp(100)

        val avrundtBeløp = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "AvrundtBelop")
        // Nytt regelsett: beløpResultat bruker avrundtBeløp (ny mellomberegning) som hvisSann
        val nyttRegelsett =
            vilkår("TestNytt") {
                regel(a) { innhentes }
                regel(direkteBeløp) { innhentes }
                regel(nullBeløp) { somUtgangspunkt(Beløp(0)) }
                regel(avrundtBeløp) { avrund(direkteBeløp) }
                regel(beløpResultat) { hvisSannMedResultat(a, avrundtBeløp, nullBeløp) }
            }

        // "Omgjøring"
        val nyeOpplysninger = Opplysninger.basertPå(opplysninger)

        nyeOpplysninger.leggTil(Faktum(direkteBeløp, Beløp(150.5)))
        val nyRegelkjøring = Regelkjøring(23.mai(2024), nyeOpplysninger, nyttRegelsett)

        // kaster IllegalStateException("Har ikke opplysning AvrundtBelop som er gyldig")
        shouldNotThrowAny { nyRegelkjøring.evaluer() }

        nyeOpplysninger.finnOpplysning(beløpResultat).verdi shouldBe Beløp(151)
    }

    @Nested
    inner class MedKunResultatSomØnsket {
        private val opplysninger = Opplysninger()
        private val regelkjøring = lagRegelkjøring(opplysninger)

        @Test
        fun `ber ikke om grenen som ikke velges, heller ikke når resultatet allerede finnes`() {
            opplysninger.leggTil(Faktum(a, true))
            opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))

            regelkjøring.evaluer().mangler shouldBe emptySet()
            val rapport = regelkjøring.evaluer()

            rapport.mangler shouldBe emptySet()
            rapport.kjørteRegler.size shouldBe 0
            opplysninger.finnOpplysning(resultat).verdi shouldBe 100.0
        }

        @Test
        fun `utledningen inneholder kun sjekk og valgt gren`() {
            opplysninger.leggTil(Faktum(a, false))
            opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
            opplysninger.leggTil(Faktum(hvisUsannVerdi, 50.0))
            regelkjøring.evaluer()

            opplysninger
                .finnOpplysning(resultat)
                .utledetAv!!
                .opplysninger
                .map { it.opplysningstype } shouldBe
                listOf(a, hvisUsannVerdi)
        }

        @Test
        fun `ber om valgt gren når sjekk snur`() {
            opplysninger.leggTil(Faktum(a, true))
            opplysninger.leggTil(Faktum(hvisSannVerdi, 100.0))
            regelkjøring.evaluer()

            opplysninger.leggTil(Faktum(a, false))
            regelkjøring.evaluer().mangler shouldBe setOf(hvisUsannVerdi)
        }

        @Test
        fun `sjekk uten produsent blokkerer regelen uten å feile`() {
            val opplysninger = Opplysninger()
            val rapport = lagRegelkjøring(opplysninger, medProdusentForSjekk = false).evaluer()

            rapport.kjørteRegler shouldBe emptySet()
            opplysninger.mangler(resultat) shouldBe true
        }

        private fun lagRegelkjøring(
            opplysninger: Opplysninger,
            medProdusentForSjekk: Boolean = true,
        ): Regelkjøring {
            val sjekkRegelsett =
                vilkår("TestSjekk") {
                    skalVurderes { medProdusentForSjekk }
                    regel(a) { innhentes }
                }
            val regelsett =
                vilkår("TestKunResultat") {
                    regel(hvisSannVerdi) { innhentes }
                    regel(hvisUsannVerdi) { innhentes }
                    regel(resultat) { hvisSannMedResultat(a, hvisSannVerdi, hvisUsannVerdi) }
                    ønsketResultat(resultat)
                }
            val prosess =
                object : Forretningsprosess(Regelverk(navn = RegelverkType("Test"), regelsett = arrayOf(sjekkRegelsett, regelsett))) {
                    override fun regelkjøring(opplysninger: Opplysninger) = throw NotImplementedError()

                    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = 23.mai(2024)

                    override fun ønsketResultat(opplysninger: LesbarOpplysninger) = setOf<Opplysningstype<*>>(resultat)
                }
            return Regelkjøring(23.mai(2024), Regelkjøring.Periode(23.mai(2024)), opplysninger, prosess)
        }
    }
}
