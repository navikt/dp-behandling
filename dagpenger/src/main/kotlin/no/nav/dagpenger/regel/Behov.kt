package no.nav.dagpenger.regel

import kotlin.reflect.KProperty

object Behov {
    val InntektId by StringConstant()
    val OpptjeningsperiodeFraOgMed by StringConstant()
    val SisteAvsluttendeKalenderMåned by StringConstant()

    val KanJobbeDeltid by StringConstant()
    val KanJobbeHvorSomHelst by StringConstant()
    val HelseTilAlleTyperJobb by StringConstant()
    val VilligTilÅBytteYrke by StringConstant()

    val RegistrertSomArbeidssøker by StringConstant()

    val Ordinær by StringConstant()
    val Permittert by StringConstant()
    val Lønnsgaranti by StringConstant()
    val PermittertFiskeforedling by StringConstant()
    val TarUtdanningEllerOpplæring by StringConstant()

    val Verneplikt by StringConstant()

    val HarTaptArbeid by StringConstant()
    val KravPåLønn by StringConstant()
}

class StringConstant {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ) = property.name
}
