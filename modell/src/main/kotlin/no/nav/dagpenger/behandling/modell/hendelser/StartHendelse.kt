package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Baseklasse for alle hendelser som kan påvirke dagpengene til en person og må behandles
abstract class StartHendelse(
    val meldingsreferanseId: UUID,
    val ident: String,
    val eksternId: EksternId<*>,
    val skjedde: LocalDate,
    val fagsakId: Int,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet) {
    val type: String = this.javaClass.simpleName

    override fun kontekstMap() =
        mapOf(
            "gjelderDato" to skjedde.toString(),
        ) + eksternId.kontekstMap()

    abstract val forretningsprosess: Forretningsprosess
    val regelverk: Regelverk
        get() = forretningsprosess.regelverk

    abstract fun regelkjøring(opplysninger: Opplysninger): Regelkjøring

    abstract fun behandling(forrigeBehandling: Behandling?): Behandling

    abstract fun kontrollpunkter(): List<Kontrollpunkt>

    abstract fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean
}
