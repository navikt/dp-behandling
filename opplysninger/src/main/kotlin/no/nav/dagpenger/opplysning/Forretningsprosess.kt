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

    fun kjørUnderOpprettelse(kontekst: Prosesskontekst) = plugins.forEach { it.underOpprettelse(kontekst) }

    fun kjørEtterRegelkjøring(kontekst: Prosesskontekst) = plugins.forEach { it.etterRegelkjøring(kontekst) }

    fun kjørRegelkjøringFerdig(kontekst: Prosesskontekst) = plugins.forEach { it.regelkjøringFerdig(kontekst) }

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
    fun underOpprettelse(kontekst: Prosesskontekst) {}

    fun etterRegelkjøring(kontekst: Prosesskontekst) {}

    fun regelkjøringFerdig(kontekst: Prosesskontekst) {}
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
