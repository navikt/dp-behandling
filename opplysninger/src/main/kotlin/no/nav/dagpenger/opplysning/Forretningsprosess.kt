package no.nav.dagpenger.opplysning

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
        regelverk.regelsett
            // .filter { it.skalKjøres(opplysninger) }
            .flatMapTo(mutableSetOf()) { it.ønsketInformasjon }

    open fun kontrollpunkter(): List<IKontrollpunkt> = emptyList()

    open fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean = false

    open fun rettighetsperioder(opplysninger: LesbarOpplysninger) = regelverk.rettighetsperioder(opplysninger)

    fun regelsett() = regelverk.regelsett

    fun registrer(handler: ProsessPlugin) = plugins.add(handler)

    fun kjørUnderOpprettelse(kontekst: Prosesskontekst) = plugins.forEach { it.underOpprettelse(kontekst) }

    fun kjørEtterRegelkjøring(kontekst: Prosesskontekst) = plugins.forEach { it.etterRegelkjøring(kontekst) }

    fun kjørRegelkjøringFerdig(kontekst: Prosesskontekst) = plugins.forEach { it.regelkjøringFerdig(kontekst) }

    fun produsenter(regelverksdato: LocalDate): Map<Opplysningstype<out Any>, Regel<*>> =
        regelverk
            .regelsett
            .flatMap { it.regler(regelverksdato) }
            .associateBy { it.produserer }

    fun reglerSomIkkeSkalKjøres(opplysningerPåPrøvingsdato: LesbarOpplysninger): Set<Regel<*>> {
        val regelsettSomIkkeSkalKjøres =
            regelverk
                .regelsett
                .filterNot {
                    it.skalKjøres(opplysningerPåPrøvingsdato).also { bool ->
                        if (!bool) println("skalKjøres er FALSE for regelsett $it")
                    } &&
                        it.skalRevurderes(opplysningerPåPrøvingsdato).also { bool ->
                            if (!bool) println("skalKjøres er TRUE,  men skalRevurderes er FALSE for regelsett $it")
                        }
                }

        regelverk
            .regelsett
            .map { Triple(it.navn, it.skalKjøres(opplysningerPåPrøvingsdato), it.skalRevurderes(opplysningerPåPrøvingsdato)) }
            .printTabell()

        return regelsettSomIkkeSkalKjøres.flatMap { it.regler() }.toSet()
    }
}

fun List<Triple<String, Boolean, Boolean>>.printTabell() {
    val headers = listOf("Regelsett", "Skal kjøres", "Skal revurderes")
    val rows =
        map { (navn, skalKjøres, skalRevurderes) ->
            listOf(navn, if (skalKjøres) "✅" else "❌", if (skalRevurderes) "✅" else "❌")
        }
    val widths =
        headers.indices.map { col ->
            maxOf(headers[col].length, rows.maxOfOrNull { it[col].length } ?: 0)
        }

    fun linje() = "+-" + widths.joinToString("-+-") { "-".repeat(it) } + "-+"

    fun rad(celler: List<String>) = "| " + celler.mapIndexed { i, v -> v.padEnd(widths[i]) }.joinToString(" | ") + " |"

    println(linje())
    println(rad(headers))
    println(linje())
    rows.forEach { println(rad(it)) }
    println(linje())
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
