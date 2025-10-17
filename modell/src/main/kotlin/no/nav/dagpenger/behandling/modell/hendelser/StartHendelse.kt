package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.TemporalCollection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Baseklasse for alle hendelser som kan påvirke dagpengene til en person og må behandles
abstract class StartHendelse(
    val meldingsreferanseId: UUID,
    val ident: String,
    val eksternId: EksternId<*>,
    val skjedde: LocalDate,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet) {
    open val type: String = this.javaClass.simpleName

    fun erSammeType(hendelse: StartHendelse): Boolean = this.type == hendelse.type

    override fun kontekstMap() =
        mapOf(
            "gjelderDato" to skjedde.toString(),
        ) + eksternId.kontekstMap()

    abstract val forretningsprosess: Forretningsprosess

    abstract fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling?
}
