package no.nav.dagpenger.opplysning

import io.opentelemetry.api.GlobalOpenTelemetry
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

abstract class Forretningsprosess(
    val regelverk: Regelverk,
) {
    private val plugins: MutableList<ProsessPlugin> = mutableListOf()

    val navn: String get() = this.javaClass.simpleName

    abstract fun regelkjøring(opplysninger: Opplysninger): Regelkjøring

    abstract fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate

    open fun ønsketResultat(opplysninger: LesbarOpplysninger): Set<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMapTo(mutableSetOf()) {
            it.ønsketInformasjon
        }

    open fun kontrollpunkter(): List<IKontrollpunkt> = emptyList()

    open fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean = false

    open fun rettighetsperioder(opplysninger: LesbarOpplysninger) = regelverk.rettighetsperioder(opplysninger)

    fun regelsett() = regelverk.regelsett

    fun registrer(handler: ProsessPlugin) = plugins.add(handler)

    fun kjørUnderOpprettelse(kontekst: Prosesskontekst) =
        medSpan("forretningsprosess.kjørUnderOpprettelse", navn) { plugins.forEach { it.underOpprettelse(kontekst) } }

    fun kjørEtterRegelkjøring(kontekst: Prosesskontekst) =
        medSpan("forretningsprosess.kjørEtterRegelkjøring", navn) { plugins.forEach { it.etterRegelkjøring(kontekst) } }

    fun kjørRegelkjøringFerdig(kontekst: Prosesskontekst) =
        medSpan("forretningsprosess.kjørRegelkjøringFerdig", navn) { plugins.forEach { it.regelkjøringFerdig(kontekst) } }

    open fun produsenter(
        regelverksdato: LocalDate,
        opplysningerPåPrøvingsdato: LesbarOpplysninger,
    ): Map<Opplysningstype<out Any>, Regel<*>> =
        regelverk.regelsett
            .filter { it.skalKjøres(opplysningerPåPrøvingsdato) }
            .filter { it.skalRevurderes(opplysningerPåPrøvingsdato) }
            .flatMap { it.regler(regelverksdato) }
            .associateBy { it.produserer }

    private companion object {
        private val tracer = GlobalOpenTelemetry.getTracer(Forretningsprosess::class.java.name)

        private fun medSpan(
            spanNavn: String,
            prosessNavn: String,
            block: () -> Unit,
        ) {
            val span = tracer.spanBuilder(spanNavn).setAttribute("prosess", prosessNavn).startSpan()
            try {
                span.makeCurrent().use { block() }
            } finally {
                span.end()
            }
        }
    }
}

interface ProsessPlugin : Aktivitetskontekst {
    fun underOpprettelse(kontekst: Prosesskontekst) {}

    fun etterRegelkjøring(kontekst: Prosesskontekst) {}

    fun regelkjøringFerdig(kontekst: Prosesskontekst) {}
}

data class Prosesskontekst(
    val opplysninger: Opplysninger,
    private val aktivitetslogg: IAktivitetslogg = Aktivitetslogg(),
) : Aktivitetskontekst,
    IAktivitetslogg by aktivitetslogg {
    init {
        aktivitetslogg.kontekst(this)
    }

    var kreverRekjøring: Boolean = false
        private set

    fun beOmRekjøring() {
        kreverRekjøring = true
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        SpesifikkKontekst(
            "Prosesskontekst",
        )
}

class Prosessregister {
    private val forretningsprosesser = mutableMapOf<String, Forretningsprosess>()

    fun registrer(forretningsprosess: Forretningsprosess) {
        forretningsprosesser[forretningsprosess.navn] = forretningsprosess
    }

    fun opprett(string: String): Forretningsprosess =
        forretningsprosesser[string] ?: throw IllegalArgumentException("Ukjent forretningsprosess: $string")
}
