package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.modell.hendelser.OpplysningSvar
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Barnekilde
import tools.jackson.databind.JsonNode
import java.util.UUID

class OpplysningSvarBygger<T : Any>(
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
        fun <T : Any> map(datatype: Datatype<T>): T
    }
}

// Barn kan komme inn i to former:
//  - V1 (gammel/utgått): en ren liste av barn, uten søknadbarnId
//  - V2 (gjeldende): et objekt med søknadbarnId og en liste av barn
// Formen kjennes igjen på JSON-strukturen, så vi trenger ikke vite hvilket
// behov/typeNavn svaret kom fra for å tolke det riktig.
fun barnMapper(verdi: String): BarnListe = barnMapper(objectMapper.readTree(verdi))

fun barnMapper(verdi: JsonNode): BarnListe =
    when {
        verdi.isArray -> BarnListe(barn = verdi.toList().map { it.tilBarn() })
        else ->
            BarnListe(
                søknadbarnId = verdi["søknadbarnId"]?.takeUnless { it.isNull }?.asUUID(),
                barn = verdi["barn"].toList().map { it.tilBarn() },
            )
    }

private fun JsonNode.tilBarn() =
    Barn(
        kilde = this["kilde"]?.asString()?.let { Barnekilde.valueOf(it) },
        ident = this["ident"]?.asString(),
        fødselsdato = this["fødselsdato"].asLocalDate(),
        fornavnOgMellomnavn = this["fornavnOgMellomnavn"]?.asString(),
        etternavn = this["etternavn"]?.asString(),
        statsborgerskap = this["statsborgerskap"]?.asString(),
        oppholdsland = this["oppholdsland"]?.asString() ?: this["statsborgerskap"]?.asString(),
        kvalifiserer = this["kvalifiserer"].asBoolean(),
        forsørgeransvar = this["forsørgeransvar"]?.asBoolean() ?: this["kvalifiserer"].asBoolean(),
        begrunnelse = this["begrunnelse"]?.asString(),
    )
