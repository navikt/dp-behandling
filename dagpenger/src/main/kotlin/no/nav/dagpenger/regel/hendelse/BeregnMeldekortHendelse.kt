package no.nav.dagpenger.regel.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
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
import no.nav.dagpenger.regel.Meldekortprosess
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDateTime
import java.util.UUID

class BeregnMeldekortHendelse(
    meldingsreferanseId: UUID,
    ident: String,
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

        val opplysninger =
            listOf(
                Faktum(
                    hendelseTypeOpplysningstype,
                    type,
                    Gyldighetsperiode.kun(skjedde),
                    kilde = Systemkilde(meldingsreferanseId, opprettet),
                ),
            ) +
                meldekort.tilOpplysninger(kilde)

        return Behandling(
            basertPå = forrigeBehandling,
            behandler = this,
            opplysninger = opplysninger,
            avklaringer =
                buildList {
                    if (meldekort.korrigeringAv != null) {
                        add(
                            Avklaring(
                                Avklaringkode(
                                    kode = "KorrigertMeldekortBehandling",
                                    tittel = "Beregning av korrigert meldekort",
                                    beskrivelse = "Behandlingen er korrigering av et tidligere meldekort og kan ikke automatisk behandles",
                                    kanAvbrytes = false,
                                ),
                            ),
                        )

                        val sisteBeregnedeDato =
                            forrigeBehandling
                                .opplysninger
                                .finnAlle(Beregning.oppfyllerKravTilTaptArbeidstidIPerioden)
                                .lastOrNull()
                        val harBeregnetPeriodenEtterDenne =
                            sisteBeregnedeDato
                                ?.gyldighetsperiode
                                ?.tilOgMed
                                ?.isAfter(meldekort.tom) == true

                        if (harBeregnetPeriodenEtterDenne) {
                            logger.error {
                                "Vi har allerede beregnet en periode etter denne meldeperioden! Dette blir en omgjøring bak i tid."
                            }
                            add(
                                Avklaring(
                                    Avklaringkode(
                                        kode = "KorrigeringUtbetaltPeriode",
                                        tittel = "Beregning av meldekort som korrigerer tidligere periode",
                                        beskrivelse = "Behandlingen er korrigering av et tidligere meldekort og kan ikke behandles",
                                        kanAvbrytes = false,
                                        kanKvitteres = false,
                                    ),
                                ),
                            )
                        }
                    } else {
                        add(
                            Avklaring(
                                Avklaringkode(
                                    kode = "MeldekortBehandling",
                                    tittel = "Beregning av meldekort",
                                    beskrivelse = "Behandlingen er opprettet av meldekort og kan ikke automatisk behandles",
                                    kanAvbrytes = false,
                                ),
                            ),
                        )
                    }

                    val aktiviteter = meldekort.dager.flatMap { it.aktiviteter }
                    if (aktiviteter.any { aktivitet -> aktivitet.type == AktivitetType.Utdanning }) {
                        add(
                            Avklaring(
                                Avklaringkode(
                                    kode = "MeldekortMedUtdanning",
                                    tittel = "Meldekort med utdanning",
                                    beskrivelse = "Bruker har krysset av for utdanning på meldekortet. Må vurderes manuelt.",
                                    kanAvbrytes = false,
                                    kanKvitteres = true,
                                ),
                            ),
                        )
                    }
                },
        ).apply {
            opplysninger.forEach { this.opplysninger.leggTil(it) }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
