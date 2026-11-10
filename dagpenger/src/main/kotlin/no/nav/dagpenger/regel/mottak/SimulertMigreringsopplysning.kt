package no.nav.dagpenger.regel.mottak

import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
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
import java.time.LocalDate

internal data class SimulertMigreringsopplysning(
    val uuid: String,
    val navn: String,
    val verdi: Any,
    val gyldigFraOgMed: String,
    val gyldigTilOgMed: String?,
)

internal fun Opplysninger.simulerteOpplysninger(): List<SimulertMigreringsopplysning> =
    somListe()
        .filterNot { it.erUtdatert }
        .sortedWith(
            compareBy(
                {
                    it.opplysningstype.id.uuid
                        .toString()
                },
                { it.gyldighetsperiode.fraOgMed },
                { it.gyldighetsperiode.tilOgMed },
            ),
        ).map { opplysning ->
            SimulertMigreringsopplysning(
                uuid =
                    opplysning.opplysningstype.id.uuid
                        .toString(),
                navn = opplysning.opplysningstype.navn,
                verdi = opplysning.opplysningstype.datatype.tilSimulertVerdi(opplysning.verdi),
                gyldigFraOgMed = opplysning.gyldighetsperiode.fraOgMed.toString(),
                gyldigTilOgMed =
                    opplysning.gyldighetsperiode.tilOgMed
                        .takeUnless { it == LocalDate.MAX }
                        ?.toString(),
            )
        }

private fun Datatype<*>.tilSimulertVerdi(verdi: Any): Any =
    when (this) {
        Boolsk -> verdi as Boolean
        Heltall -> verdi as Int
        Desimaltall -> verdi as Double
        Tekst -> verdi as String
        Dato -> (verdi as LocalDate).toString()
        Penger -> (verdi as Beløp).verdien
        ULID -> (verdi as Ulid).verdi
        PeriodeDataType ->
            (verdi as Periode).let { periode ->
                mapOf(
                    "fraOgMed" to periode.fraOgMed.toString(),
                    "tilOgMed" to periode.tilOgMed.toString(),
                )
            }
        BarnDatatype ->
            (verdi as BarnListe).let { barnListe ->
                mapOf(
                    "søknadbarnId" to barnListe.søknadbarnId?.toString(),
                    "barn" to
                        barnListe.barn.map { barn ->
                            mapOf(
                                "kilde" to barn.kilde?.name,
                                "ident" to barn.ident,
                                "fødselsdato" to barn.fødselsdato.toString(),
                                "fornavnOgMellomnavn" to barn.fornavnOgMellomnavn,
                                "etternavn" to barn.etternavn,
                                "statsborgerskap" to barn.statsborgerskap,
                                "oppholdsland" to barn.oppholdsland,
                                "kvalifiserer" to barn.kvalifiserer,
                                "forsørgeransvar" to barn.forsørgeransvar,
                                "begrunnelse" to barn.begrunnelse,
                            )
                        },
                )
            }
        InntektDataType ->
            (verdi as Inntekt).verdi.let { inntekt ->
                mapOf(
                    "inntektsId" to inntekt.inntektsId,
                    "sisteAvsluttendeKalenderMåned" to inntekt.sisteAvsluttendeKalenderMåned.toString(),
                    "manueltRedigert" to inntekt.manueltRedigert,
                    "inntektsListe" to
                        inntekt.inntektsListe.map { måned ->
                            mapOf(
                                "årMåned" to måned.årMåned.toString(),
                                "klassifiserteInntekter" to
                                    måned.klassifiserteInntekter.map { klassifisertInntekt ->
                                        mapOf(
                                            "beløp" to klassifisertInntekt.beløp,
                                            "inntektKlasse" to klassifisertInntekt.inntektKlasse.name,
                                        )
                                    },
                                "harAvvik" to måned.harAvvik,
                            )
                        },
                )
            }
    }
