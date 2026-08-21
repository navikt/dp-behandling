package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.TestOpplysningstyper.b
import no.nav.dagpenger.opplysning.TestOpplysningstyper.c
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato1
import no.nav.dagpenger.opplysning.TestOpplysningstyper.dato2
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltall
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.random.Random

/**
 * Sikkerhetsnett (steg 0) forut for ytelsesarbeid på `Opplysninger.refreshOpplysninger()`.
 *
 * Disse testene låser fast invariantene som enhver framtidig, mer inkrementell
 * implementasjon (f.eks. en per-opplysningstype-cache) MÅ bevare. De sier ingenting om
 * *hvordan* refresh er implementert, kun *hva* resultatet skal være - så de fungerer som
 * et orakel/regresjonsnett uavhengig av hvilket alternativ vi eventuelt lander på.
 */
class OpplysningerInvariantTest {
    private val typer = listOf(a, b, c, dato1, dato2, desimaltall)
    private val basisdato = LocalDate.of(2020, 1, 1)

    /**
     * Genererer en tilfeldig, men realistisk sekvens av leggTil-kall (enkeltvis og i batch)
     * på tvers av flere typer, med overlappende, tilstøtende og identiske perioder - og
     * sjekker invariantene etter HVER mutasjon, ikke bare til slutt. Dette er den type test
     * som skal fange opp subtile regresjoner introdusert av en framtidig cache-omskriving.
     */
    @RepeatedTest(50)
    fun `invarianter holder gjennom en tilfeldig sekvens av leggTil og fjern`() {
        val seed = Random.nextLong()
        val random = Random(seed)
        val opplysninger = Opplysninger()

        try {
            repeat(200) { i ->
                when (random.nextInt(4)) {
                    0, 1 ->
                        // Enkeltvis leggTil - det vanlige tilfellet
                        opplysninger.leggTil(tilfeldigOpplysning(random, i))

                    2 ->
                        // Batch leggTil av 2-4 opplysninger av ULIKE typer (siden
                        // krevIngenOverlappInnadIBatch forbyr overlapp innad i samme type)
                        opplysninger.leggTil { fakta ->
                            typer.shuffled(random).take(random.nextInt(2, 5)).forEachIndexed { j, type ->
                                fakta.add(tilfeldigOpplysningAvType(random, type, i * 10 + j))
                            }
                        }

                    else -> {
                        // Fjern en tilfeldig egen opplysning, om det finnes noen
                        val egne = opplysninger.somListe(LesbarOpplysninger.Filter.Egne)
                        if (egne.isNotEmpty()) {
                            opplysninger.fjern(egne.random(random).id)
                        }
                    }
                }

                verifiserInvarianter(opplysninger, "etter steg $i (seed=$seed)")
            }
        } catch (e: Throwable) {
            throw AssertionError("Test feilet med seed=$seed - reproduser med denne seeden", e)
        }
    }

    @Test
    fun `arv og kaskadefjerning over flere typer bevarer invarianter`() {
        val ledd1 = Opplysninger()
        ledd1.leggTil(Faktum(a, true, Gyldighetsperiode(basisdato)))
        ledd1.leggTil(Faktum(b, true, Gyldighetsperiode(basisdato)))
        verifiserInvarianter(ledd1, "ledd1")

        val ledd2 = Opplysninger.basertPå(ledd1)
        val avhengighetC = Faktum(c, true, Gyldighetsperiode(basisdato.plusDays(10)))
        ledd2.leggTil(avhengighetC)
        // 3 opplysninger utledet av samme avhengighet, spredt over 3 ulike typer
        ledd2.leggTil(Faktum(dato1, basisdato, utledetAv = Utledning("Test", listOf(avhengighetC))))
        ledd2.leggTil(Faktum(dato2, basisdato, utledetAv = Utledning("Test", listOf(avhengighetC))))
        ledd2.leggTil(Faktum(desimaltall, 1.0, utledetAv = Utledning("Test", listOf(avhengighetC))))
        verifiserInvarianter(ledd2, "ledd2 før kaskadefjerning")

        // Fjerner avhengigheten - skal kaskadere og fjerne alle 3 avhengige typer
        ledd2.fjern(avhengighetC.id)
        verifiserInvarianter(ledd2, "ledd2 etter kaskadefjerning")

        ledd2.har(dato1) shouldBe false
        ledd2.har(dato2) shouldBe false
        ledd2.har(desimaltall) shouldBe false
        // ledd1 skal være upåvirket av mutasjoner i ledd2
        ledd1.har(a) shouldBe true
        ledd1.har(b) shouldBe true
    }

    private fun verifiserInvarianter(
        opplysninger: Opplysninger,
        kontekst: String,
    ) {
        val alle = opplysninger.somListe(LesbarOpplysninger.Filter.Alle)
        val egne = opplysninger.somListe(LesbarOpplysninger.Filter.Egne)

        // 1. Den flate lista skal være strengt sortert på id.
        alle.zipWithNext().forEach { (forrige, neste) ->
            assert(forrige.id < neste.id) {
                "$kontekst: alleOpplysninger er ikke id-sortert: ${forrige.id} kommer før ${neste.id}"
            }
        }
        egne.zipWithNext().forEach { (forrige, neste) ->
            assert(forrige.id < neste.id) {
                "$kontekst: somListe(Egne) er ikke id-sortert: ${forrige.id} kommer før ${neste.id}"
            }
        }

        // 2. Ingen duplikate id-er i den flate lista (fanger evt. dobbel forkorting av opplysninger).
        val ider = alle.map { it.id }
        assert(ider.distinct().size == ider.size) {
            "$kontekst: alleOpplysninger inneholder duplikate id-er"
        }

        // 3. Ingen overlappende gyldighetsperioder innad i samme opplysningstype i det
        //    resolverte resultatet.
        alle.groupBy { it.opplysningstype }.forEach { (type, gruppe) ->
            val sortertPåFraOgMed = gruppe.sortedBy { it.gyldighetsperiode.fraOgMed }
            sortertPåFraOgMed.zipWithNext().forEach { (forrige, neste) ->
                assert(!forrige.gyldighetsperiode.overlapper(neste.gyldighetsperiode)) {
                    "$kontekst: overlappende gyldighetsperioder for ${type.navn}: " +
                        "${forrige.gyldighetsperiode} og ${neste.gyldighetsperiode}"
                }
            }
        }

        // 4. har()/finnNullableOpplysning() skal være konsistent med det resolverte settet:
        //    en type med minst én ikke-tombstonet opplysning i 'alle' skal gi har() == true.
        typer.forEach { type ->
            val forventerHar = alle.any { it.er(type) }
            assert(opplysninger.har(type) == forventerHar) {
                "$kontekst: har(${type.navn}) == ${opplysninger.har(type)}, forventet $forventerHar"
            }
        }
    }

    private fun tilfeldigOpplysning(
        random: Random,
        seq: Int,
    ): Opplysning<*> = tilfeldigOpplysningAvType(random, typer.random(random), seq)

    private fun tilfeldigOpplysningAvType(
        random: Random,
        type: Opplysningstype<*>,
        seq: Int,
    ): Opplysning<*> {
        val fraOgMed = basisdato.plusDays(random.nextLong(0, 60))
        val varighet = random.nextLong(0, 20)
        val gyldighetsperiode =
            if (random.nextInt(5) == 0) {
                Gyldighetsperiode(fraOgMed)
            } else {
                Gyldighetsperiode(fraOgMed, fraOgMed.plusDays(varighet))
            }

        return when (type) {
            a -> Faktum(a, random.nextBoolean(), gyldighetsperiode)
            b -> Faktum(b, random.nextBoolean(), gyldighetsperiode)
            c -> Faktum(c, random.nextBoolean(), gyldighetsperiode)
            dato1 -> Faktum(dato1, basisdato.plusDays(seq.toLong()), gyldighetsperiode)
            dato2 -> Faktum(dato2, basisdato.plusDays(seq.toLong()), gyldighetsperiode)
            desimaltall -> Faktum(desimaltall, random.nextDouble(0.0, 100.0), gyldighetsperiode)
            else -> error("Ukjent testtype $type")
        }
    }
}
