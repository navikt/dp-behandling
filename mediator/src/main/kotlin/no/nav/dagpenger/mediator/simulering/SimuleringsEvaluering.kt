package no.nav.dagpenger.mediator.simulering

import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.Utledning
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import java.time.LocalDate

data class EvalueringsResultat(
    val opplysninger: List<OpplysningNode>,
    val mangler: Set<String>,
)

data class OpplysningNode(
    val navn: String,
    val behovId: String,
    val datatype: String,
    val verdi: Any,
    val utledetAv: UtledningNode?,
)

data class UtledningNode(
    val regel: String,
    val avhengigheter: List<OpplysningNode>,
)

class SimuleringsEvaluering {
    fun evaluer(
        regelsett: Regelsett,
        dato: LocalDate,
        inndata: Map<String, Any?>,
    ): EvalueringsResultat {
        val opplysninger = byggOpplysninger(regelsett, dato, inndata)
        val regelkjøring = Regelkjøring(dato, opplysninger, regelsett)
        val rapport = regelkjøring.evaluer()

        val produserte =
            regelsett.produserer
                .mapNotNull { type ->
                    opplysninger.kunEgne
                        .forDato(dato)
                        .finnNullableOpplysning(type)
                }.map { it.tilNode() }

        val manglerFraEkstern = rapport.mangler.map { it.behovId }.toSet()
        val manglerFraAvhengigheter = ikkeOppgitteAvhengigheter(regelsett, opplysninger, dato)

        return EvalueringsResultat(
            opplysninger = produserte,
            mangler = manglerFraEkstern + manglerFraAvhengigheter,
        )
    }

    private fun ikkeOppgitteAvhengigheter(
        regelsett: Regelsett,
        opplysninger: Opplysninger,
        dato: LocalDate,
    ): Set<String> {
        val tilgjengelige = opplysninger.kunEgne.forDato(dato)
        return (regelsett.avhengerAv + regelsett.behov)
            .filterNot { tilgjengelige.har(it) }
            .map { it.behovId }
            .toSet()
    }

    private fun byggOpplysninger(
        regelsett: Regelsett,
        dato: LocalDate,
        inndata: Map<String, Any?>,
    ): Opplysninger {
        val typePerBehovId = (regelsett.avhengerAv + regelsett.behov).associateBy { it.behovId }
        val opplysningsliste =
            inndata.mapNotNull { (behovId, verdi) ->
                val type = typePerBehovId[behovId] ?: return@mapNotNull null
                byggFaktum(type, verdi, dato)
            }
        return opplysningsliste.somOpplysninger()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> byggFaktum(
        type: Opplysningstype<T>,
        verdi: Any?,
        dato: LocalDate,
    ): Faktum<T>? {
        if (verdi == null) return null
        val typetVerdi: T =
            when (type.datatype) {
                Dato -> LocalDate.parse(verdi.toString()) as T
                Heltall ->
                    when (verdi) {
                        is Int -> verdi as T
                        is Long -> verdi.toInt() as T
                        is Number -> verdi.toInt() as T
                        else -> verdi.toString().toInt() as T
                    }
                Desimaltall ->
                    when (verdi) {
                        is Double -> verdi as T
                        is Number -> verdi.toDouble() as T
                        else -> verdi.toString().toDouble() as T
                    }
                Boolsk ->
                    when (verdi) {
                        is Boolean -> verdi as T
                        else -> verdi.toString().toBoolean() as T
                    }
                Tekst -> verdi.toString() as T
                Penger ->
                    when (verdi) {
                        is Number -> Beløp(verdi.toDouble()) as T
                        else -> Beløp(verdi.toString().toDouble()) as T
                    }
                PeriodeDataType -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = verdi as? Map<String, String> ?: return null
                    Periode(
                        LocalDate.parse(map["fraOgMed"] ?: return null),
                        LocalDate.parse(map["tilOgMed"] ?: return null),
                    ) as T
                }
                else -> return null
            }
        return Faktum(type, typetVerdi, Gyldighetsperiode(dato, LocalDate.MAX))
    }
}

private fun Opplysning<*>.tilNode(): OpplysningNode =
    OpplysningNode(
        navn = opplysningstype.navn,
        behovId = opplysningstype.behovId,
        datatype = opplysningstype.datatype.navn(),
        verdi = serialiserVerdi(verdi),
        utledetAv = utledetAv?.tilNode(),
    )

private fun Utledning.tilNode(): UtledningNode =
    UtledningNode(
        regel = regel,
        avhengigheter = opplysninger.map { it.tilNode() },
    )

private fun serialiserVerdi(verdi: Any): Any =
    when (verdi) {
        is Beløp -> verdi.verdien
        is Periode -> mapOf("fraOgMed" to verdi.fraOgMed, "tilOgMed" to verdi.tilOgMed)
        is LocalDate -> verdi.toString()
        else -> verdi
    }
