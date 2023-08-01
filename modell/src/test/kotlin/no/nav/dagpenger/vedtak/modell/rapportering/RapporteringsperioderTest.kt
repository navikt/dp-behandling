package no.nav.dagpenger.vedtak.modell.rapportering

import de.fxlae.typeid.TypeId
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.vedtak.hjelpere.februar
import no.nav.dagpenger.vedtak.hjelpere.mai
import no.nav.dagpenger.vedtak.modell.entitet.Timer.Companion.timer
import no.nav.dagpenger.vedtak.modell.hendelser.Rapporteringsdag
import no.nav.dagpenger.vedtak.modell.hendelser.Rapporteringshendelse
import no.nav.dagpenger.vedtak.modell.visitor.PersonVisitor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

internal class RapporteringsperioderTest {
    private lateinit var rapporteringsperioder: Rapporteringsperioder

    private val inspektør get() = RapporteringsdagerVisitor(rapporteringsperioder)

    @BeforeEach
    fun setup() {
        rapporteringsperioder = Rapporteringsperioder().also {
            it.håndter(
                rapporteringshendelse(
                    Rapporteringsdag(1.februar(2023), listOf(arbeid(3.hours))),
                    Rapporteringsdag(2.februar(2023), listOf(arbeid(0.hours))),
                ),
            )
        }
    }

    @Test
    fun `kan merge to rapporteringsperioder`() {
        rapporteringsperioder.håndter(
            rapporteringshendelse(
                Rapporteringsdag(1.februar(2023), listOf(arbeid(6.hours))),
                Rapporteringsdag(2.februar(2023), listOf(arbeid(0.hours))),
            ),
        )

        val dager = inspektør.dager
        dager.size shouldBe 2
        dager.first().dato() shouldBe 1.februar(2023)
        dager.first().arbeidstimer() shouldBe 6.timer
        dager.last().dato() shouldBe 2.februar(2023)
        dager.last().arbeidstimer() shouldBe 0.timer
        inspektør.antallRapporteringsperioder shouldBe 1
    }

    @Test
    fun `kan legge til rapportering`() {
        rapporteringsperioder.håndter(
            rapporteringshendelse(
                Rapporteringsdag(3.februar(2023), listOf(arbeid(6.hours))),
                Rapporteringsdag(4.februar(2023), listOf(arbeid(0.hours))),
            ),
        )
        val dager = inspektør.dager
        dager.size shouldBe 4
        dager.first().dato() shouldBe 1.februar(2023)
        dager.last().dato() shouldBe 4.februar(2023)
        inspektør.antallRapporteringsperioder shouldBe 2
    }

    @Test
    fun `skal bevare historikk`() {
        val mai = (1 until 32).map { dag -> dag.mai(2023) }
        val maiTilRapporteringsperioder = mai.partition { it < 14.mai(2023) }
        val førsteRapporteringsHendelse =
            maiTilRapporteringsperioder.first.map { Rapporteringsdag(it, listOf(arbeid(Random.nextInt(0, 8).hours))) }
        val andreRapporteringsHendelse =
            maiTilRapporteringsperioder.second.map { Rapporteringsdag(it, listOf(arbeid(Random.nextInt(0, 8).hours))) }
        rapporteringsperioder.håndter(
            rapporteringshendelse(*førsteRapporteringsHendelse.toTypedArray()),
        )
        rapporteringsperioder.håndter(
            rapporteringshendelse(*andreRapporteringsHendelse.toTypedArray()),
        )

        val dager = inspektør.dager
        dager.size shouldBe 33
        dager.first().dato() shouldBe 1.februar(2023)
        dager.last().dato() shouldBe 31.mai(2023)
        inspektør.antallRapporteringsperioder shouldBe 3
    }

    private fun rapporteringshendelse(vararg rapporteringsdager: Rapporteringsdag) = Rapporteringshendelse(
        ident = "123",
        rapporteringsId = UUID.randomUUID(),
        rapporteringsdager = rapporteringsdager.toList(),
        fom = rapporteringsdager.minOf { it.dato },
        tom = rapporteringsdager.maxOf { it.dato },

    )

    private fun arbeid(tid: Duration) = Rapporteringsdag.Aktivitet(Rapporteringsdag.Aktivitet.Type.Arbeid, tid)

    private class RapporteringsdagerVisitor(rapporteringsperioder: Rapporteringsperioder) : PersonVisitor {

        val dager = mutableListOf<Dag>()
        var antallRapporteringsperioder = 0

        init {
            rapporteringsperioder.accept(this)
        }

        override fun preVisitRapporteringsperiode(rapporteringsperiodeId: TypeId, periode: Rapporteringsperiode) {
            antallRapporteringsperioder++
        }

        override fun visitDag(dag: Dag, aktiviteter: List<Aktivitet>) {
            dager.add(dag)
        }
    }
}
