package no.nav.dagpenger.regel

import kotlin.reflect.KProperty

object Behov {
    val Søknadsdato by StringConstant()
    val ØnskerDagpengerFraDato by StringConstant()
    val Prøvingsdato by StringConstant()

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
    val Inntekt by StringConstant()
    val ØnsketArbeidstid by StringConstant()

    val Barnetillegg by StringConstant()

    val Sykepenger by StringConstant()
    val Pleiepenger by StringConstant()
    val Omsorgspenger by StringConstant()
    val Opplæringspenger by StringConstant()
    val Uføre by StringConstant()
    val Foreldrepenger by StringConstant()
    val Svangerskapspenger by StringConstant()

    val OppgittAndreYtelserUtenforNav by StringConstant()
    val AndreØkonomiskeYtelser by StringConstant()

    val BostedslandErNorge by StringConstant()
    val OppholdINorge by StringConstant()
}

class StringConstant {
    operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ) = property.name
}
