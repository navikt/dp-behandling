package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.roundToInt

class BeregnMeldekortHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    meldekortId: UUID,
    opprettet: LocalDateTime,
    private val meldekort: Meldekort,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = MeldekortId(meldekort.eksternMeldekortId),
        skjedde = meldekort.innsendtTidspunkt.toLocalDate(),
        opprettet = opprettet,
    ) {
    override val forretningsprosess = Søknadsprosess()

    override val regelverk: Regelverk
        get() = forretningsprosess.regelverk

    // TODO: DETTE ER HELT FEIL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate =
        if (opplysninger.har(Søknadstidspunkt.prøvingsdato)) {
            opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi
        } else {
            throw IllegalStateException("Fant ikke prøvingsdato")
        }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring =
        Regelkjøring(
            regelverksdato = prøvingsdato(opplysninger = opplysninger),
            opplysninger = opplysninger,
            forretningsprosess = forretningsprosess,
        )

    override fun behandling(forrigeBehandling: Behandling?): Behandling {
        requireNotNull(forrigeBehandling) { "Må ha en behandling å ta utgangspunkt i" }
        val kilde = Systemkilde(meldekort.meldingsreferanseId, opprettet)

        return Behandling(
            behandler = this,
            basertPå = listOf(forrigeBehandling),
            opplysninger =
                listOf(
                    Faktum(
                        hendelseTypeOpplysningstype,
                        type,
                        gyldighetsperiode = Gyldighetsperiode(fom = skjedde),
                        kilde = Systemkilde(meldingsreferanseId, opprettet),
                    ),
                    Faktum(
                        Beregning.meldeperiode,
                        Periode(meldekort.fom, meldekort.tom),
                        Gyldighetsperiode(meldekort.fom, meldekort.tom),
                        kilde = kilde,
                    ),
                ) +
                    meldekort.dager.flatMap { dag ->
                        val gyldighetsperiode = Gyldighetsperiode(dag.dato, dag.dato)

                        // TODO: Dette bør være en double
                        val timer = dag.aktiviteter.sumOf { it.timer?.inWholeHours ?: 0 }.toInt()
                        // TODO: Hva om det er flere aktiviteter?
                        val type = dag.aktiviteter.firstOrNull()?.type
                        when (type) {
                            AktivitetType.Arbeid -> {
                                listOf(
                                    Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde),
                                    Faktum(Beregning.arbeidstimer, timer, gyldighetsperiode, kilde = kilde),
                                )
                            }

                            AktivitetType.Syk,
                            AktivitetType.Utdanning,
                            AktivitetType.Fravær,
                            -> listOf(Faktum(Beregning.arbeidsdag, false, gyldighetsperiode, kilde = kilde))

                            null -> {
                                listOf(
                                    Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde),
                                    Faktum(Beregning.arbeidstimer, 0, gyldighetsperiode, kilde = kilde),
                                )
                            }
                        } + Faktum(Beregning.meldt, dag.meldt, gyldighetsperiode, kilde = kilde)
                    },
        ).apply {
            val fabrikk = BeregningsperiodeFabrikk(meldekort.fom, meldekort.tom, this.opplysninger())
            val periode = fabrikk.lagBeregningsperiode()

            periode.forbruksdager.forEach {
                val gyldighetsperiode = Gyldighetsperiode(it.dato, it.dato)
                opplysninger.leggTil(Faktum(Beregning.utbetaling, it.tilUtbetaling.roundToInt(), gyldighetsperiode))
            }
        }
    }

    override fun kontrollpunkter(): List<Kontrollpunkt> = emptyList()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean = false

    // TODO: Finne riktig dato i en meldekort behandling
    override fun virkningsdato(opplysninger: LesbarOpplysninger) = prøvingsdato(opplysninger)

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> = emptyList()
}
