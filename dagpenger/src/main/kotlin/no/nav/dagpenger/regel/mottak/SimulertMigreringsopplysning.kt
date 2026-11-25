package no.nav.dagpenger.regel.mottak

import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.Ulid
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import java.time.LocalDate

internal data class SimulertMigreringsopplysning(
    val opplysningTypeId: String,
    val navn: String,
    val datatype: String,
    val perioder: List<Map<String, Any>>,
)

internal fun Opplysninger.simulerteOpplysninger(): List<SimulertMigreringsopplysning> =
    somListe()
        .groupBy { it.opplysningstype }
        .map { (type, opplysninger) ->
            SimulertMigreringsopplysning(
                opplysningTypeId = type.id.uuid.toString(),
                navn = type.navn,
                datatype = type.datatype.tilSimulertDatatype(),
                perioder = opplysninger.map { it.tilSimulertPeriodeDTO(::erEgen) },
            )
        }

private fun Opplysning<*>.tilSimulertPeriodeDTO(erEgen: (Opplysning<*>) -> Boolean): Map<String, Any> =
    wireObject(
        "id" to id.toString(),
        "opprettet" to opprettet,
        "opprinnelse" to if (erEgen(this)) "Ny" else "Arvet",
        "gyldigFraOgMed" to gyldighetsperiode.fraOgMed.tilSimulertDato(),
        "gyldigTilOgMed" to gyldighetsperiode.tilOgMed.tilSimulertDato(),
        "verdi" to tilSimulertVerdi(),
    )

private fun Opplysning<*>.tilSimulertVerdi(): Map<String, Any> =
    when (opplysningstype.datatype) {
        Boolsk -> verdiDTO(verdi as Boolean, "boolsk")
        Dato -> verdiDTO((verdi as LocalDate).toString(), "dato")
        Desimaltall ->
            wireObject(
                "verdi" to (verdi as Double),
                "datatype" to "desimaltall",
                "enhet" to opplysningstype.enhet?.tilSimulertEnhet(),
            )
        Heltall ->
            wireObject(
                "verdi" to (verdi as Int),
                "datatype" to "heltall",
                "enhet" to opplysningstype.enhet?.tilSimulertEnhet(),
            )
        InntektDataType -> verdiDTO((verdi as Inntekt).verdi.inntektsId, "tekst")
        Penger -> verdiDTO((verdi as Beløp).verdien, "penger")
        PeriodeDataType ->
            (verdi as Periode).let {
                wireObject(
                    "fom" to it.fraOgMed.toString(),
                    "tom" to it.tilOgMed.toString(),
                    "datatype" to "periode",
                )
            }
        Tekst -> verdiDTO(verdi as String, "tekst")
        ULID -> verdiDTO((verdi as Ulid).verdi, "tekst")
        BarnDatatype ->
            (verdi as BarnListe).let { barnListe ->
                wireObject(
                    "søknadBarnId" to barnListe.søknadbarnId?.toString(),
                    "verdi" to
                        barnListe.barn.map { barn ->
                            wireObject(
                                "fødselsdato" to barn.fødselsdato.toString(),
                                "fornavnOgMellomnavn" to barn.fornavnOgMellomnavn,
                                "etternavn" to barn.etternavn,
                                "statsborgerskap" to barn.statsborgerskap,
                                "oppholdsland" to barn.oppholdsland,
                                "forsørgeransvar" to barn.forsørgeransvar,
                                "kvalifiserer" to barn.kvalifiserer,
                                "begrunnelse" to barn.begrunnelse,
                            )
                        },
                    "datatype" to "barn",
                )
            }
    }

private fun <T : Any> verdiDTO(
    verdi: T,
    datatype: String,
): Map<String, Any> =
    mapOf(
        "verdi" to verdi,
        "datatype" to datatype,
    )

private fun Datatype<*>.tilSimulertDatatype(): String =
    when (this) {
        Boolsk -> "boolsk"
        Dato -> "dato"
        Desimaltall -> "desimaltall"
        Heltall -> "heltall"
        ULID -> "ulid"
        Penger -> "penger"
        InntektDataType -> "inntekt"
        BarnDatatype -> "barn"
        Tekst -> "tekst"
        PeriodeDataType -> "periode"
    }

private fun Enhet.tilSimulertEnhet(): String =
    when (this) {
        Enhet.G -> "G"
        Enhet.Prosent -> "prosent"
        Enhet.Timer -> "timer"
        Enhet.Dager -> "dager"
        Enhet.Uker -> "uker"
        Enhet.År -> "år"
        Enhet.Måneder -> "måneder"
    }

private fun LocalDate.tilSimulertDato(): String? = takeUnless { it == LocalDate.MIN || it == LocalDate.MAX }?.toString()

private fun wireObject(vararg fields: Pair<String, Any?>): Map<String, Any> =
    fields.mapNotNull { (name, value) -> value?.let { name to it } }.toMap()
