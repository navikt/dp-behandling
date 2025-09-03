package no.nav.dagpenger.opplysning

import java.time.LocalDate

interface Forretningsprosess {
    val regelverk: Regelverk
    val navn: String get() = this.javaClass.simpleName

    fun regelkjøring(opplysninger: Opplysninger): Regelkjøring

    fun kontrollpunkter(): List<IKontrollpunkt>

    fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean

    fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate

    fun utfall(opplysninger: LesbarOpplysninger) = regelverk.utfall(opplysninger.forDato(virkningsdato(opplysninger)))

    fun regelsett() = regelverk.regelsett

    fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>>

    fun klatten(
        tilstand: Klatteland,
        opplysninger: Opplysninger,
    ) {}
}

enum class Klatteland {
    Start,
    Underveis,
    Ferdig,
}

abstract class RegistrertForretningsprosess : Forretningsprosess {
    fun registrer() {
        registrer(navn, this)
    }

    companion object {
        private val forretningsprosesser = mutableMapOf<String, Forretningsprosess>()

        private fun registrer(
            type: String,
            forretningsprosess: Forretningsprosess,
        ) {
            forretningsprosesser[type] = forretningsprosess
        }

        fun opprett(string: String): Forretningsprosess =
            forretningsprosesser[string] ?: throw IllegalArgumentException("Ukjent forretningsprosess: $string")
    }
}
