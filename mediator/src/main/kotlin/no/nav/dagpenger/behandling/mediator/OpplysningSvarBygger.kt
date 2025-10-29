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
import java.util.UUID

class OpplysningSvarBygger<T : Comparable<T>>(
    private val type: Opplysningstype<T>,
    private val verdi: VerdiMapper,
    private val kilde: Kilde,
    private val tilstand: OpplysningSvar.Tilstand,
    private val gyldighetsperiode: Gyldighetsperiode?,
    private val utledetAv: List<UUID>,
    private val utledetAvRegelsett: String?,
) {
    fun opplysningSvar() =
        OpplysningSvar(
            opplysningstype = type,
            verdi = verdi.map(type.datatype),
            tilstand = tilstand,
            kilde = kilde,
            gyldighetsperiode = gyldighetsperiode,
            utledetAv = utledetAv,
            utledetAvRegelsett = utledetAvRegelsett,
        )

    interface VerdiMapper {
        fun <T : Comparable<T>> map(datatype: Datatype<T>): T
    }
}

fun barnMapper(verdi: String): BarnListe = barnMapper(objectMapper.readTree(verdi))

fun barnMapper(verdi: JsonNode): BarnListe =
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
