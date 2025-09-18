package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Rettighetsperiode
import java.time.LocalDate

typealias BehandlingLoader = () -> Behandling?

sealed class Rettighetsforhold(
    val rettighetsperioder: Set<Rettighetsperiode>,
    @Transient private val loader: BehandlingLoader,
) {
    constructor(rettighetsperioder: Set<Rettighetsperiode>, behandling: Behandling?) : this(rettighetsperioder, { behandling })

    val behandling by lazy { loader() }

    open fun erLøpende(): Boolean = true

    fun harRettighet(dato: LocalDate) = rettighetsperioder.any { it.gjelder(dato) && it.harRett }
}

class LøpendeRettighetsforhold(
    rettighetsperioder: Set<Rettighetsperiode>,
    behandling: Behandling,
) : Rettighetsforhold(rettighetsperioder, behandling)

class IngenRettighetsforhold(
    behandling: Behandling?,
) : Rettighetsforhold(emptySet(), behandling) {
    override fun erLøpende(): Boolean = false
}

class Ppperson(
    val ident: Ident,
    var rettighetsforhold: Rettighetsforhold,
) : BehandlingObservatør {
    constructor(ident: Ident) : this(ident, IngenRettighetsforhold(null))

    fun harRettighet(dato: LocalDate) = rettighetsforhold.harRettighet(dato)

    fun håndter(hendelse: StartHendelse): Behandling =
        hendelse.behandling(rettighetsforhold.behandling).also { behandling ->
            behandling.registrer(this)
            behandling.håndter(hendelse)
        }

    override fun ferdig(event: BehandlingFerdig) {
        if (event.rettighetsperioder.size == 1 && !event.rettighetsperioder.single().harRett) {
            rettighetsforhold = IngenRettighetsforhold(event.behandling)
            return
        }

        val nyePerioder = event.rettighetsperioder.subtract(rettighetsforhold.rettighetsperioder)
        rettighetsforhold = LøpendeRettighetsforhold(event.rettighetsperioder, event.behandling)

        println(nyePerioder)
    }
}
