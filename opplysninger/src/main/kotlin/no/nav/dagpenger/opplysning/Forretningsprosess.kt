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

    fun kjørStart(opplysninger: Opplysninger) = plugins.forEach { it.start(opplysninger) }

    fun kjørUnderveis(opplysninger: Opplysninger) = plugins.forEach { it.underveis(opplysninger) }

    fun kjørFerdig(opplysninger: Opplysninger) = plugins.forEach { it.ferdig(opplysninger) }

    open fun produsenter(
        regelverksdato: LocalDate,
        opplysningerPåPrøvingsdato: LesbarOpplysninger,
    ) = regelverk.regelsett
        .filter { it.skalKjøres(opplysningerPåPrøvingsdato) }
        .flatMap { it.regler(regelverksdato) }
        .associateBy { it.produserer }
}

interface ProsessPlugin {
    fun start(opplysninger: Opplysninger) {}

    fun underveis(opplysninger: Opplysninger) {}

    fun ferdig(opplysninger: Opplysninger) {}
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
