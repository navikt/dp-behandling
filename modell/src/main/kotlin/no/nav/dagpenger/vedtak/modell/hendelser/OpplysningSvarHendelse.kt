package no.nav.dagpenger.vedtak.modell.hendelser

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import java.util.UUID

class OpplysningSvarHendelse(meldingsreferanseId: UUID, ident: String, val behandlingId: UUID, val opplysninger: List<OpplysningSvar<*>>) :
    PersonHendelse(meldingsreferanseId, ident)

data class OpplysningSvar<T : Comparable<T>>(
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val tilstand: Tilstand,
) {
    enum class Tilstand {
        Hypotese,
        Faktum,
    }

    fun opplysning(): Opplysning<T> {
        return when (tilstand) {
            Tilstand.Hypotese -> Hypotese(opplysningstype, verdi)
            Tilstand.Faktum -> Faktum(opplysningstype, verdi)
        }
    }
}
