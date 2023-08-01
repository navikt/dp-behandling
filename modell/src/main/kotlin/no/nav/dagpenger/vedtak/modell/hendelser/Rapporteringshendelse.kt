package no.nav.dagpenger.vedtak.modell.hendelser

import de.fxlae.typeid.TypeId
import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.vedtak.modell.entitet.Periode
import no.nav.dagpenger.vedtak.modell.entitet.Timer.Companion.timer
import no.nav.dagpenger.vedtak.modell.hendelser.Rapporteringsdag.Aktivitet.Type.Arbeid
import no.nav.dagpenger.vedtak.modell.hendelser.Rapporteringsdag.Aktivitet.Type.Ferie
import no.nav.dagpenger.vedtak.modell.hendelser.Rapporteringsdag.Aktivitet.Type.Syk
import no.nav.dagpenger.vedtak.modell.rapportering.Dag
import no.nav.dagpenger.vedtak.modell.rapportering.Rapporteringsperiode
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration

class Rapporteringshendelse(
    ident: String,
    internal val rapporteringsId: UUID,
    rapporteringsdager: List<Rapporteringsdag>,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    private val fom: LocalDate,
    private val tom: LocalDate,
) : Hendelse(ident, aktivitetslogg), ClosedRange<LocalDate> {
    private val rapporteringsdager = rapporteringsdager.sorted()

    override val endInclusive: LocalDate = tom
    override val start: LocalDate = fom
    internal fun populerRapporteringsperiode(): Rapporteringsperiode {
        return Rapporteringsperiode(TypeId.of(Rapporteringsperiode.idPrefix, rapporteringsId), Periode(fom, tom)).also { periode ->
            rapporteringsdager.forEach { rapporteringdag ->
                val aktiviteter = rapporteringdag.aktiviteter.map {
                    when (it.type) {
                        Arbeid -> no.nav.dagpenger.vedtak.modell.rapportering.Arbeid(it.varighet.timer)
                        Syk -> no.nav.dagpenger.vedtak.modell.rapportering.Syk(it.varighet.timer)
                        Ferie -> no.nav.dagpenger.vedtak.modell.rapportering.Ferie(it.varighet.timer)
                    }
                }
                val dag = Dag.opprett(rapporteringdag.dato, aktiviteter)
                periode.leggTilDag(dag)
            }
        }
    }
    override fun kontekstMap(): Map<String, String> = mapOf("rapporteringsId" to rapporteringsId.toString())
}

class Rapporteringsdag(val dato: LocalDate, val aktiviteter: List<Aktivitet>) : Comparable<Rapporteringsdag> {
    override fun compareTo(other: Rapporteringsdag) = this.dato.compareTo(other.dato)

    class Aktivitet(val type: Type, val varighet: Duration) {

        enum class Type {
            Arbeid, Syk, Ferie
        }
    }
}
