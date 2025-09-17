package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Rettighetsperiode
import java.time.LocalDate

open class Rettighetsforhold(
    val rettighetsperiode: List<Rettighetsperiode>,
    val behandling: Behandling?,
) {
    open fun erLøpende(): Boolean = true

    fun harRettighet(dato: LocalDate) = rettighetsperiode.any { it.gjelder(dato) && it.harRett }
}

class IngenRettighetsforhold : Rettighetsforhold(emptyList(), null) {
    override fun erLøpende(): Boolean = false
}

data class Ppppppperson(
    val ident: Ident,
    var rettighetsforhold: Rettighetsforhold,
) : BehandlingObservatør {
    fun harRettighet(dato: LocalDate) = rettighetsforhold.harRettighet(dato)

    fun håndter(hendelse: StartHendelse): Behandling =
        hendelse.behandling(rettighetsforhold.behandling).also { behandling ->
            behandling.håndter(hendelse)
        }

    override fun ferdig(event: BehandlingFerdig) {
        if (event.rettighetsperioder.size == 1 && !event.rettighetsperioder.single().harRett) {
            rettighetsforhold = IngenRettighetsforhold()
            return
        }
        // rettighetsforhold = Rettighetsforhold(event.rettighetsperioder, event.behandlingId)
    }
}
