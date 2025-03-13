package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Forretningsprosess
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class EnHvilkenSomHelstHendelse(
    meldingsreferanseId: UUID,
    hendelseType: String,
    ident: String,
    eksternId: EksternId<*>,
    skjedde: LocalDate,
    opprettet: LocalDateTime,
    override val forretningsprosess: Forretningsprosess,
) : StartHendelse(meldingsreferanseId, ident, eksternId, skjedde, opprettet),
    Forretningsprosess by forretningsprosess {
    override fun behandling(forrigeBehandling: Behandling?): Behandling =
        throw IllegalStateException("Skal ikke opprettet behandling her, skal allerede ha skjedd")
}

// Baseklasse for alle hendelser som kan påvirke dagpengene til en person og må behandles
abstract class StartHendelse(
    val meldingsreferanseId: UUID,
    val ident: String,
    val eksternId: EksternId<*>,
    val skjedde: LocalDate,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    Forretningsprosess {
    val type: String = this.javaClass.simpleName

    override fun kontekstMap() =
        mapOf(
            "gjelderDato" to skjedde.toString(),
        ) + eksternId.kontekstMap()

    abstract val forretningsprosess: Forretningsprosess

    abstract fun behandling(forrigeBehandling: Behandling?): Behandling
}
