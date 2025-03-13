package no.nav.dagpenger.opplysning

interface Forretningsprosess {
    val regelverk: Regelverk
    val navn: String get() = this.javaClass.simpleName

    fun regelkjøring(opplysninger: Opplysninger): Regelkjøring

    fun kontrollpunkter(): List<IKontrollpunkt>

    fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean

    fun regelsett() = regelverk.regelsett

    fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>>
}

abstract class RegistrertForretningsprosess : Forretningsprosess {
    init {
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
