package no.nav.dagpenger.opplysning

interface Forretningsprosess {
    val regelverk: Regelverk

    fun regelkjøring(opplysninger: Opplysninger): Regelkjøring

    fun kontrollpunkter(): List<IKontrollpunkt>

    fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean

    fun regelsett() = regelverk.regelsett

    fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>>
}
