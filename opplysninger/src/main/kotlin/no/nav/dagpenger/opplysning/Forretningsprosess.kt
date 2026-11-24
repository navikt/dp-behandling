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

    open fun kanOppfriske(
        opplysninger: LesbarOpplysninger,
        opplysningstype: Opplysningstype<*>,
    ) = regelverk.kanOppfriske(opplysningstype) && opplysninger.kunEgne.har(opplysningstype)

    fun regelsett() = regelverk.regelsett

    fun registrer(handler: ProsessPlugin) = plugins.add(handler)

    fun kjørUnderOpprettelse(kontekst: Prosesskontekst) =
        tracer.medSpan("forretningsprosess.kjørUnderOpprettelse", mapOf("prosess" to navn)) {
            plugins.forEach { it.underOpprettelse(kontekst) }
        }

    fun kjørEtterRegelkjøring(kontekst: Prosesskontekst) =
        tracer.medSpan("forretningsprosess.kjørEtterRegelkjøring", mapOf("prosess" to navn)) {
            plugins.forEach { it.etterRegelkjøring(kontekst) }
        }

    fun kjørRegelkjøringFerdig(kontekst: Prosesskontekst) =
        tracer.medSpan("forretningsprosess.kjørRegelkjøringFerdig", mapOf("prosess" to navn)) {
            plugins.forEach { it.regelkjøringFerdig(kontekst) }
        }

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

    private val rekjøringshistorikk = mutableListOf<Historikkpost>()

    private val rekjøringer get() = rekjøringshistorikk.filterIsInstance<Historikkpost.Rekjøring>()

    val kreverRekjøring: Boolean get() = rekjøringshistorikk.lastOrNull() is Historikkpost.Rekjøring

    val antallRekjøringerRunder: Int get() = rekjøringer.map { it.runde }.distinct().size

    val rekjøringerPerÅrsak: Map<String, Int> get() = rekjøringer.groupingBy { it.årsak }.eachCount()

    fun startNyRunde() {
        check(antallRekjøringerRunder <= MAKS_REKJØRINGER) {
            val årsaker = rekjøringerPerÅrsak.toSortedMap().entries.joinToString { "${it.key}=${it.value}" }
            "Fanget i uendelig løkke: Regler og plugins blir aldri enige. Rekjøringsårsaker: $årsaker"
        }
        rekjøringshistorikk.add(Historikkpost.Startet)
    }

    fun beOmRekjøring(årsak: String) {
        val runde = rekjøringshistorikk.count { it is Historikkpost.Startet }
        val post = Historikkpost.Rekjøring(runde, årsak)
        if (post in rekjøringshistorikk) return

        rekjøringshistorikk.add(post)
        val antall = rekjøringerPerÅrsak.getValue(årsak)
        info("$årsak har bedt om rekjøring $antall ganger.")
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        SpesifikkKontekst(
            "Prosesskontekst",
        )

    private companion object {
        const val MAKS_REKJØRINGER = 10
    }

    private sealed interface Historikkpost {
        data object Startet : Historikkpost

        data class Rekjøring(
            val runde: Int,
            val årsak: String,
        ) : Historikkpost
    }
}

class Prosessregister {
    private val forretningsprosesser = mutableMapOf<String, Forretningsprosess>()

    fun registrer(forretningsprosess: Forretningsprosess) {
        forretningsprosesser[forretningsprosess.navn] = forretningsprosess
    }

    fun opprett(string: String): Forretningsprosess =
        forretningsprosesser[string] ?: throw IllegalArgumentException("Ukjent forretningsprosess: $string")
}
