package no.nav.dagpenger.vedtak.modell.hendelser

import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.vedtak.modell.Dagpengerettighet
import no.nav.dagpenger.vedtak.modell.entitet.Beløp
import no.nav.dagpenger.vedtak.modell.entitet.Stønadsdager
import no.nav.dagpenger.vedtak.modell.entitet.Timer
import no.nav.dagpenger.vedtak.modell.vedtak.Avslag.Companion.avslag
import no.nav.dagpenger.vedtak.modell.vedtak.Rammevedtak.Companion.innvilgelse
import no.nav.dagpenger.vedtak.modell.vedtak.Vedtak
import java.time.LocalDate
import java.util.UUID

sealed class SøknadBehandletHendelse(
    protected val ident: String,
    internal val behandlingId: UUID,
    protected val virkningsdato: LocalDate,
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : Hendelse(ident, aktivitetslogg) {
    abstract fun tilVedtak(): Vedtak
}

class DagpengerInnvilgetHendelse(
    ident: String,
    behandlingId: UUID,
    virkningsdato: LocalDate,
    private val dagpengerettighet: Dagpengerettighet,
    private val dagsats: Beløp,
    private val grunnlag: Beløp,
    private val stønadsdager: Stønadsdager,
    private val vanligArbeidstidPerDag: Timer,
    private val egenandel: Beløp,
) : SøknadBehandletHendelse(
    ident,
    behandlingId,
    virkningsdato,
) {
    override fun tilVedtak(): Vedtak = innvilgelse(
        behandlingId = behandlingId,
        virkningsdato = virkningsdato,
        grunnlag = grunnlag,
        dagsats = dagsats,
        stønadsdager = stønadsdager,
        dagpengerettighet = dagpengerettighet,
        vanligArbeidstidPerDag = vanligArbeidstidPerDag,
        egenandel = egenandel,
    )

    override fun kontekstMap(): Map<String, String> = emptyMap()
}

class DagpengerAvslåttHendelse(ident: String, behandlingId: UUID, virkningsdato: LocalDate) :
    SøknadBehandletHendelse(
        ident,
        behandlingId,
        virkningsdato,
    ) {
    override fun tilVedtak(): Vedtak = avslag(behandlingId, virkningsdato)
    override fun kontekstMap(): Map<String, String> = emptyMap()
}
