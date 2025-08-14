package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.tilTimer
import no.nav.dagpenger.regel.Meldekortprosess
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.Beregning.utbetaling
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDateTime
import java.util.UUID

class BeregnMeldekortHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    meldekortId: UUID,
    opprettet: LocalDateTime,
    private val meldekort: Meldekort,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = meldekort.eksternMeldekortId,
        skjedde = meldekort.innsendtTidspunkt.toLocalDate(),
        opprettet = opprettet,
    ) {
    override val forretningsprosess = Meldekortprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        requireNotNull(forrigeBehandling) { "Må ha en behandling å ta utgangspunkt i" }
        val kilde = Systemkilde(meldekort.meldingsreferanseId, opprettet)
        logger.info { "Baserer meldekortberegning på: ${forrigeBehandling.behandlingId}" }

        return Behandling(
            basertPå = forrigeBehandling,
            behandler = this,
            opplysninger =
                listOf(
                    Faktum(
                        hendelseTypeOpplysningstype,
                        type,
                        gyldighetsperiode = Gyldighetsperiode(fom = meldekort.fom, tom = meldekort.tom),
                        kilde = Systemkilde(meldingsreferanseId, opprettet),
                    ),
                    Faktum(
                        Beregning.meldeperiode,
                        Periode(meldekort.fom, meldekort.tom),
                        Gyldighetsperiode(meldekort.fom, meldekort.tom),
                        kilde = kilde,
                    ),
                ),
            avklaringer =
                listOf(
                    Avklaring(
                        Avklaringkode(
                            kode = "MeldekortBehandling",
                            tittel = "Beregning av meldekort",
                            beskrivelse = "Behandlingen er opprettet av meldekort og kan ikke automatisk behandles",
                            kanAvbrytes = false,
                        ),
                    ),
                ),
        ).apply {
            meldekort.dager.forEach { dag ->
                val gyldighetsperiode = Gyldighetsperiode(dag.dato, dag.dato)

                val timer = dag.aktiviteter.map { it.timer?.tilTimer ?: Timer(0) }.summer()
                // TODO: Hva om det er flere aktiviteter?
                val type = dag.aktiviteter.firstOrNull()?.type
                when (type) {
                    AktivitetType.Arbeid -> {
                        listOf(
                            opplysninger.leggTil(Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde)),
                            opplysninger.leggTil(Faktum(Beregning.arbeidstimer, timer.timer, gyldighetsperiode, kilde = kilde)),
                        )
                    }

                    AktivitetType.Syk,
                    AktivitetType.Utdanning,
                    AktivitetType.Fravær,
                    -> opplysninger.leggTil(Faktum(Beregning.arbeidsdag, false, gyldighetsperiode, kilde = kilde))

                    null -> {
                        opplysninger.leggTil(
                            Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde),
                        )
                        opplysninger.leggTil(
                            Faktum(Beregning.arbeidstimer, 0.0, gyldighetsperiode, kilde = kilde),
                        )
                    }
                }
                opplysninger.leggTil(Faktum(Beregning.meldt, dag.meldt, gyldighetsperiode, kilde = kilde))
            }
            val fabrikk = BeregningsperiodeFabrikk(meldekort.fom, meldekort.tom, this.opplysninger(), rettighetstatus)
            val periode = fabrikk.lagBeregningsperiode()
            val forbruksdager = periode.forbruksdager

            // Kjør regler

            meldekort
                .periode()
                .forEach { dato ->
                    val forbruksdag = forbruksdager.singleOrNull { it.dato.isEqual(dato) }
                    val gyldighetsperiode = Gyldighetsperiode(dato, dato)

                    val tilUtbetaling = forbruksdag?.avrundetUtbetaling ?: 0

                    // TODO: Denne vil vi skal være desimaltall - her må vi endre på opplysningstype
                    val forbruktEgenandel = forbruksdag?.forbruktEgenandel?.avrundet?.toInt() ?: 0

                    val erForbruk = tilUtbetaling > 0 || forbruktEgenandel > 0
                    opplysninger.leggTil(Faktum(forbruk, erForbruk, gyldighetsperiode))
                    opplysninger.leggTil(Faktum(utbetaling, tilUtbetaling, gyldighetsperiode))
                    opplysninger.leggTil(Faktum(Beregning.forbruktEgenandel, forbruktEgenandel, gyldighetsperiode))
                }
        }
    }

    companion object {
        private val logger = mu.KotlinLogging.logger {}
    }
}
