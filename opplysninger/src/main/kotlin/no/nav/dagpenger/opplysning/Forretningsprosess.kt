package no.nav.dagpenger.opplysning

import java.time.LocalDate

abstract class Forretningsprosess(
    val regelverk: Regelverk,
) {
    private val plugins: MutableList<ProsessPlugin> = mutableListOf()

    val navn: String get() = this.javaClass.simpleName

    abstract fun regelkjøring(opplysninger: Opplysninger): Regelkjøring

    abstract fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate

    abstract fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>>

    open fun kontrollpunkter(): List<IKontrollpunkt> = emptyList()

    open fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean = false

    open fun rettighetsperioder(opplysninger: LesbarOpplysninger) = regelverk.rettighetsperioder(opplysninger)

    fun regelsett() = regelverk.regelsett

    fun registrer(handler: ProsessPlugin) = plugins.add(handler)

    fun kjørStart(kontekst: Prosesskontekst) = plugins.forEach { it.start(kontekst) }

    fun kjørUnderveis(kontekst: Prosesskontekst) = plugins.forEach { it.underveis(kontekst) }

    fun kjørFerdig(kontekst: Prosesskontekst) = plugins.forEach { it.ferdig(kontekst) }

    open fun produsenter(
        regelverksdato: LocalDate,
        opplysningerPåPrøvingsdato: LesbarOpplysninger,
    ) = regelverk.regelsett
        .filter { it.skalKjøres(opplysningerPåPrøvingsdato) }
        .filter { it.skalRevurderes(opplysningerPåPrøvingsdato) }
        .flatMap { it.regler(regelverksdato) }
        .associateBy { it.produserer }
}

interface ProsessPlugin {
    fun start(kontekst: Prosesskontekst) {}

    fun underveis(kontekst: Prosesskontekst) {}

    fun ferdig(kontekst: Prosesskontekst) {}
}

data class Prosesskontekst(
    val opplysninger: Opplysninger,
) {
    var kreverRekjøring: Boolean = false
        private set

    fun beOmRekjøring() {
        kreverRekjøring = true
    }
}

class Prosessregister {
    companion object {
        val RegistrertForretningsprosess = Prosessregister()
    }

    private val forretningsprosesser = mutableMapOf<String, Forretningsprosess>()

    fun registrer(forretningsprosess: Forretningsprosess) {
        forretningsprosesser[forretningsprosess.navn] = forretningsprosess
    }

    fun opprett(string: String): Forretningsprosess =
        forretningsprosesser[string] ?: throw IllegalArgumentException("Ukjent forretningsprosess: $string")
}
