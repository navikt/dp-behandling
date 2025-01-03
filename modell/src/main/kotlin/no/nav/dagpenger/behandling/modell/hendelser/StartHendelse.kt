package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
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

    abstract fun regelkjøring(opplysninger: Opplysninger): Regelkjøring

    abstract fun avklarer(opplysninger: LesbarOpplysninger): Opplysningstype<*>

    abstract fun behandling(): Behandling

    abstract fun kontrollpunkter(): List<Kontrollpunkt>

    abstract fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate

    // TODO: Disse er midlertidlige til vi kan skru på innvilgelse som normalt
    abstract fun støtterInnvilgelse(opplysninger: LesbarOpplysninger): Boolean

    abstract fun kravPåDagpenger(opplysninger: LesbarOpplysninger): Boolean

    abstract fun minsteinntekt(opplysninger: LesbarOpplysninger): Boolean

    abstract fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean
}
