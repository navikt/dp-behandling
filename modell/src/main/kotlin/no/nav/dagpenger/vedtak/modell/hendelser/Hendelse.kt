package no.nav.dagpenger.vedtak.modell.hendelser

import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import java.time.LocalDate
import java.util.UUID

abstract class Hendelse protected constructor(
    private val meldingsreferanseId: UUID,
    private val ident: String,
    private val aktivitetslogg: IAktivitetslogg,
) : AktivitetsloggHendelse, IAktivitetslogg by aktivitetslogg {
    abstract val gjelderDato: LocalDate

    init {
        aktivitetslogg.kontekst(this)
    }

    final override fun ident() = ident

    final override fun toSpesifikkKontekst() =
        SpesifikkKontekst(
            this.javaClass.simpleName,
            mapOf(
                "meldingsreferanseId" to meldingsreferanseId.toString(),
                "ident" to ident,
            ) + kontekst(),
        )

    fun toLogString(): String = aktivitetslogg.toString()

    final override fun meldingsreferanseId() = meldingsreferanseId

    protected open fun kontekst(): Map<String, String> = emptyMap()
}
