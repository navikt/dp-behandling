package no.nav.dagpenger.modell

import no.nav.dagpenger.opplysning.Saksbehandler
import java.time.LocalDateTime

class Arbeidssteg private constructor(
    val oppgave: Oppgave,
    private var tilstand: ArbeidsstegTilstand,
) {
    constructor(oppgave: Oppgave) : this(oppgave, IkkeUtførtArbeidssteg())

    val erUtført get() = tilstand.type == TilstandType.Utført
    val tilstandType get() = tilstand.type
    val utførtAv: Saksbehandler get() = tilstand.utførtAv
    val utført: LocalDateTime get() = tilstand.utført

    fun erUtførtAv(utførtAv: Saksbehandler) = tilstand.erUtførtAv(utførtAv)

    companion object {
        fun rehydrer(
            tilstand: TilstandType,
            oppgave: Oppgave,
            utførtAv: Saksbehandler? = null,
            utført: LocalDateTime? = null,
        ) = when (tilstand) {
            TilstandType.IkkeUtført -> Arbeidssteg(oppgave, IkkeUtførtArbeidssteg())
            TilstandType.Utført -> Arbeidssteg(oppgave, UtførtArbeidssteg(utførtAv!!, utført!!))
        }
    }

    internal fun utførtAv(saksbehandler: Saksbehandler) {
        tilstand = UtførtArbeidssteg(saksbehandler)
    }

    internal fun ikkeUtført() {
        tilstand = IkkeUtførtArbeidssteg()
    }

    private interface ArbeidsstegTilstand {
        val utførtAv: Saksbehandler
        val utført: LocalDateTime
        val type: TilstandType

        fun erUtførtAv(utførtAv: Saksbehandler): Boolean
    }

    private class IkkeUtførtArbeidssteg : ArbeidsstegTilstand {
        override val utførtAv get() = throw IllegalStateException("Arbeidssteg er ikke utført")
        override val utført get() = throw IllegalStateException("Arbeidssteg er ikke utført")
        override val type = TilstandType.IkkeUtført

        override fun erUtførtAv(utførtAv: Saksbehandler) = false
    }

    private data class UtførtArbeidssteg(
        override val utførtAv: Saksbehandler,
        override val utført: LocalDateTime = LocalDateTime.now(),
    ) : ArbeidsstegTilstand {
        override val type = TilstandType.Utført

        override fun erUtførtAv(utførtAv: Saksbehandler) = utførtAv == this.utførtAv
    }

    enum class Oppgave {
        Godkjent,
        Besluttet,
    }

    enum class TilstandType {
        IkkeUtført,
        Utført,
    }
}
