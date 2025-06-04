package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
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
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.Søknadsprosess
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
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

    private val logger = mu.KotlinLogging.logger {}
    override val regelverk: Regelverk
        get() = forretningsprosess.regelverk

    // TODO: DETTE ER HELT FEIL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate =
        if (opplysninger.har(Søknadstidspunkt.prøvingsdato)) {
            opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi
        } else {
            throw IllegalStateException("Fant ikke prøvingsdato")
        }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val prøvingsdato = prøvingsdato(opplysninger = opplysninger)
        val førsteDagMedRett = maxOf(prøvingsdato, meldekort.fom)

        // TODO: Vi trenger også en smartere måte å finne stansdato

        return Regelkjøring(
            regelverksdato = prøvingsdato,
            prøvingsperiode = Regelkjøring.Periode(start = førsteDagMedRett, endInclusive = meldekort.tom),
            opplysninger = opplysninger,
            forretningsprosess = forretningsprosess,
        )
    }

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        requireNotNull(forrigeBehandling) { "Må ha en behandling å ta utgangspunkt i" }
        val kilde = Systemkilde(meldekort.meldingsreferanseId, opprettet)
        logger.info { "Baserer meldekortberegning på: ${forrigeBehandling.behandlingId}" }

        return Behandling(
            behandler = this,
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
                ),
            basertPå = listOf(forrigeBehandling),
        ).apply {
            meldekort.dager.forEach { dag ->
                val gyldighetsperiode = Gyldighetsperiode(dag.dato, dag.dato)

                // TODO: Dette bør være en double
                val timer = dag.aktiviteter.sumOf { it.timer?.inWholeHours ?: 0 }.toInt()
                // TODO: Hva om det er flere aktiviteter?
                val type = dag.aktiviteter.firstOrNull()?.type
                when (type) {
                    AktivitetType.Arbeid -> {
                        listOf(
                            opplysninger.leggTil(Faktum(Beregning.arbeidsdag, true, gyldighetsperiode, kilde = kilde)),
                            opplysninger.leggTil(Faktum(Beregning.arbeidstimer, timer, gyldighetsperiode, kilde = kilde)),
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
                            Faktum(Beregning.arbeidstimer, 0, gyldighetsperiode, kilde = kilde),
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
                    val tilUtbetaling = forbruksdager.singleOrNull { it.dato == dato }?.tilUtbetaling?.roundToInt() ?: 0
                    val gyldighetsperiode = Gyldighetsperiode(dato, dato)

                    val erForbruk = tilUtbetaling > 0
                    opplysninger.leggTil(Faktum(forbruk, erForbruk, gyldighetsperiode))
                    opplysninger.leggTil(Faktum(Beregning.utbetaling, tilUtbetaling, gyldighetsperiode))
                }
        }
    }

    override fun kontrollpunkter(): List<Kontrollpunkt> = emptyList()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean = false

    // TODO: Finne riktig dato i en meldekort behandling
    override fun virkningsdato(opplysninger: LesbarOpplysninger) = prøvingsdato(opplysninger)

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> = emptyList()
}
