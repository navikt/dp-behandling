package no.nav.dagpenger.behandling.mediator

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvar
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.regel.Behov.Barnetillegg
import no.nav.dagpenger.regel.Behov.BarnetilleggV2
import java.util.UUID
import kotlin.text.get

class OpplysningSvarBygger<T : Comparable<T>>(
    private val type: Opplysningstype<T>,
    private val verdi: VerdiMapper,
    private val kilde: Kilde,
    private val tilstand: OpplysningSvar.Tilstand,
    private val gyldighetsperiode: Gyldighetsperiode?,
    private val utledetAv: List<UUID>,
) {
    fun opplysningSvar() =
        OpplysningSvar(
            opplysningstype = type,
            verdi = verdi.map(type.datatype),
            tilstand = tilstand,
            kilde = kilde,
            gyldighetsperiode = gyldighetsperiode,
            utledetAv = utledetAv,
        )

    interface VerdiMapper {
        fun <T : Comparable<T>> map(datatype: Datatype<T>): T
    }
}

fun barnMapper(verdi: String): BarnListe = barnMapper(BarnetilleggV2, objectMapper.readTree(verdi))

fun barnMapper(
    typeNavn: String,
    verdi: JsonNode,
): BarnListe =
    when (typeNavn) {
        Barnetillegg ->
            BarnListe(
                barn =
                    verdi.map {
                        Barn(
                            fødselsdato = it["fødselsdato"].asLocalDate(),
                            fornavnOgMellomnavn = it["fornavnOgMellomnavn"]?.asText(),
                            etternavn = it["etternavn"]?.asText(),
                            statsborgerskap = it["statsborgerskap"]?.asText(),
                            kvalifiserer = it["kvalifiserer"].asBoolean(),
                        )
                    },
            )
        BarnetilleggV2 -> {
            BarnListe(
                søknadbarnId = verdi["søknadbarnId"]?.asText()?.let { UUID.fromString(it) },
                barn =
                    verdi["barn"].map {
                        Barn(
                            fødselsdato = it["fødselsdato"].asLocalDate(),
                            fornavnOgMellomnavn = it["fornavnOgMellomnavn"]?.asText(),
                            etternavn = it["etternavn"]?.asText(),
                            statsborgerskap = it["statsborgerskap"]?.asText(),
                            kvalifiserer = it["kvalifiserer"].asBoolean(),
                        )
                    },
            )
        }
        else -> throw IllegalArgumentException("Ukjent typeNavn for barnMapper: $typeNavn")
    }
