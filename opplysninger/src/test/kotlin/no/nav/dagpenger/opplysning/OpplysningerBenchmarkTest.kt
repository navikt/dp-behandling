package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.TestOpplysningstyper.b
import no.nav.dagpenger.opplysning.TestOpplysningstyper.c
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.measureTime

/**
 * Mikrobenchmark (steg 0) - måler, men ASSERTERER IKKE på absolutte tall (unngår flaky CI).
 *
 * Formål: gi en rask, reproduserbar baseline å sammenligne mot når
 * `refreshOpplysninger()` gjøres mer inkrementell (se analysen fra 2026-08-21 om
 * per-opplysningstype-cache). Kjør med `-Dopplysninger.benchmark.log=true` eller les
 * output fra en vanlig testkjøring - tidene skrives alltid til stdout.
 *
 * Testene er holdt raske nok (millisekunder-få sekunder) til å kunne kjøres som del av
 * den vanlige `test`-oppgaven uten å bremse CI merkbart.
 */
class OpplysningerBenchmarkTest {
    private val basisdato: LocalDate = LocalDate.of(2020, 1, 1)

    @Test
    fun `mange enkeltvise leggTil med lesning mellom hver - typisk regelkjøringsmønster`() {
        val antallTyper = 200
        val alleTyper = lagOpplysningstyper(antallTyper)
        val opplysninger = Opplysninger()

        val tid =
            measureTime {
                repeat(500) { i ->
                    val type: Opplysningstype<Boolean> = alleTyper[i % antallTyper]
                    opplysninger.leggTil(
                        Faktum<Boolean>(type, true, Gyldighetsperiode(basisdato.plusDays(i.toLong()))),
                    )
                    // Simulerer at regelmotoren leser rett etter hver innsetting
                    opplysninger.har(type)
                }
            }

        loggResultat(
            "500 enkeltvise leggTil + lesning, $antallTyper typer, ${opplysninger.somListe().size} opplysninger",
            tid,
        )
    }

    @Test
    fun `stor batch leggTil, én lesning til slutt`() {
        val antallTyper = 500
        val alleTyper = lagOpplysningstyper(antallTyper)
        val opplysninger = Opplysninger()

        val tid =
            measureTime {
                opplysninger.leggTil { fakta ->
                    alleTyper.forEachIndexed { i, type ->
                        fakta.add(Faktum<Boolean>(type, true, Gyldighetsperiode(basisdato.plusDays(i.toLong()))))
                    }
                }
                opplysninger.somListe().size
            }

        loggResultat(
            "1 batch med 500 leggTil, $antallTyper typer, ${opplysninger.somListe().size} opplysninger",
            tid,
        )
    }

    @Test
    fun `lang arvekjede over flere behandlingsledd`() {
        val boolskeTyper = listOf(a, b, c)
        var siste = Opplysninger()
        boolskeTyper.forEach { type -> siste.leggTil(Faktum(type, true, Gyldighetsperiode(basisdato))) }

        val tid =
            measureTime {
                repeat(10) { ledd ->
                    siste = Opplysninger.basertPå(siste)
                    repeat(50) { i ->
                        val type = boolskeTyper[i % boolskeTyper.size]
                        siste.leggTil(
                            Faktum(type, true, Gyldighetsperiode(basisdato.plusDays((ledd * 50 + i).toLong()))),
                        )
                    }
                }
                siste.somListe().size
            }

        loggResultat("10 behandlingsledd, 50 leggTil per ledd", tid)
    }

    private fun lagOpplysningstyper(antall: Int): List<Opplysningstype<Boolean>> =
        (0 until antall).map { i ->
            Opplysningstype.boolsk(
                Opplysningstype.Id(
                    no.nav.dagpenger.uuid.UUIDv7
                        .ny(),
                    Boolsk,
                ),
                "BenchmarkType$i",
            )
        }

    private fun loggResultat(
        beskrivelse: String,
        tid: kotlin.time.Duration,
    ) {
        println("[benchmark] $beskrivelse: $tid")
    }
}
