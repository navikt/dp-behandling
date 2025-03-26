package no.nav.dagpenger.opplysning

interface Forretningsprosess<T : Regelverkstype> {
    val regelverk: Regelverk<T>
    val navn: String get() = this.javaClass.simpleName

    fun regelkjøring(opplysninger: Opplysninger): Regelkjøring

    fun kontrollpunkter(): List<IKontrollpunkt>

    fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean

    fun regelsett() = regelverk.regelsett

    fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>>

    fun somVedtak(opplysninger: LesbarOpplysninger): Vedtak<T> = TODO()
}

abstract class RegistrertForretningsprosess<T : Regelverkstype> : Forretningsprosess<T> {
    fun registrer() {
        registrer(navn, this)
    }

    companion object {
        private val forretningsprosesser = mutableMapOf<String, Forretningsprosess<*>>()

        private fun registrer(
            type: String,
            forretningsprosess: Forretningsprosess<*>,
        ) {
            forretningsprosesser[type] = forretningsprosess
        }

        fun opprett(string: String): Forretningsprosess<*> =
            forretningsprosesser[string] ?: throw IllegalArgumentException("Ukjent forretningsprosess: $string")
    }
}
