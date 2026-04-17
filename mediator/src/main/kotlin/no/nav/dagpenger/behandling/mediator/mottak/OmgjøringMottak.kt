package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.modell.hendelser.OmgjøringId
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.hendelse.OmgjøringHendelse
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime

internal class OmgjøringMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: IMessageMediator,
    private val meldekortRepository: MeldekortRepository,
    private val opplysningstyper: Set<Opplysningstype<*>>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "omgjør_behandling") }
                validate {
                    it.requireKey("ident")
                    it.requireKey("gjelderDato")
                    it.interestedIn("initialOpplysninger")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val message = OmgjøringMessage(packet, meldekortRepository, opplysningstyper)
        message.behandle(hendelseMediator, context)
    }
}

internal class OmgjøringMessage(
    packet: JsonMessage,
    meldekortRepository: MeldekortRepository,
    opplysningstyper: Set<Opplysningstype<*>>,
) : KafkaMelding(packet) {
    override val ident: String = packet["ident"].asText()
    private val kilde = Systemkilde(id, opprettet)
    private val hendelse =
        OmgjøringHendelse(
            meldingsreferanseId = id,
            ident = ident,
            eksternId = OmgjøringId(UUIDv7.ny()),
            gjelderDato = packet["gjelderDato"].asLocalDate(),
            opprettet = opprettet,
            initialOpplysninger = parseInitialOpplysninger(packet, opplysningstyper, kilde, opprettet),
        )

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}

@Suppress("UNCHECKED_CAST")
private fun parseInitialOpplysninger(
    packet: JsonMessage,
    opplysningstyper: Set<Opplysningstype<*>>,
    kilde: Systemkilde,
    opprettet: LocalDateTime,
): List<Faktum<*>> {
    val node = packet["initialOpplysninger"]
    if (node.isMissingNode || node.isNull) return emptyList()

    return node.map { opplysningNode ->
        val typeId = opplysningNode["opplysningstype"].asUUID()
        val type =
            opplysningstyper.singleOrNull { it.id.uuid == typeId }
                ?: throw IllegalArgumentException("Ukjent opplysningstype: $typeId")
        val verdi = opplysningNode["verdi"].asText()
        val gyldigFraOgMed = opplysningNode["gyldigFraOgMed"]?.takeIf { !it.isMissingNode && !it.isNull }?.asLocalDate()
        val gyldighetsperiode = gyldigFraOgMed?.let { Gyldighetsperiode(it) } ?: Gyldighetsperiode()
        type.tilFaktum(verdi, kilde, gyldighetsperiode, opprettet)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Comparable<T>> Opplysningstype<T>.tilFaktum(
    verdi: String,
    kilde: Systemkilde,
    gyldighetsperiode: Gyldighetsperiode,
    opprettet: LocalDateTime,
): Faktum<T> {
    val typedVerdi: T =
        when (datatype) {
            Penger -> Beløp(verdi.toBigDecimal()) as T
            Dato -> LocalDate.parse(verdi) as T
            Heltall -> verdi.toInt() as T
            Desimaltall -> verdi.toDouble() as T
            Boolsk -> verdi.toBoolean() as T
            Tekst -> verdi as T
            else -> throw IllegalArgumentException("Datatype $datatype støttes ikke for initialOpplysninger")
        }
    return Faktum(
        opplysningstype = this,
        verdi = typedVerdi,
        gyldighetsperiode = gyldighetsperiode,
        kilde = kilde,
        opprettet = opprettet,
    )
}
