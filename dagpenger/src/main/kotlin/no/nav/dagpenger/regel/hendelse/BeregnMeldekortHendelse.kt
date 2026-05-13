package no.nav.dagpenger.regel.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.hendelser.AktivitetType
import no.nav.dagpenger.modell.hendelser.Dag
import no.nav.dagpenger.modell.hendelser.Meldekort
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.Opprettet
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.prosess.Meldekortprosess
import no.nav.dagpenger.regel.prosess.Omgjøringsprosess
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.antallBarn
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.godkjentUnntakForUtdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.tarUtdanning
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
    override val forretningsprosess get() = if (harBeregnetPeriodenEtterDenne) Omgjøringsprosess() else Meldekortprosess()

    private var harBeregnetPeriodenEtterDenne: Boolean = false

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat {
        requireNotNull(forrigeBehandling) { "Må ha en behandling å ta utgangspunkt i" }
        logger.info { "Baserer meldekortberegning på: ${forrigeBehandling.behandlingId}" }
        val kilde = Systemkilde(meldekort.meldingsreferanseId, opprettet)
        val saksbehandlerKilde =
            when (meldekort.kilde.rolle) {
                "Saksbehandler" -> {
                    Saksbehandlerkilde(
                        meldingsreferanseId = meldekort.meldingsreferanseId,
                        saksbehandler = Saksbehandler(meldekort.kilde.ident),
                        opprettet = opprettet,
                    )
                }

                else -> {
                    kilde
                }
            }
        val behandling =
            opprettBehandling(
                basertPå = forrigeBehandling,
                avklaringer =
                    buildList {
                        val sisteBeregnedeMeldeperiode = forrigeBehandling.opplysninger.finnAlle(Beregning.meldeperiode).lastOrNull()
                        val sisteBeregnedeDato = sisteBeregnedeMeldeperiode?.gyldighetsperiode?.tilOgMed

                        harBeregnetPeriodenEtterDenne = sisteBeregnedeDato?.let { sisteBeregnedeDato >= meldekort.tom } ?: false

                        logger.info { "Har valgt ${forretningsprosess.javaClass.simpleName} som prosess for å beregne meldekort" }

                        if (meldekort.korrigeringAv != null) {
                            if (harBeregnetPeriodenEtterDenne) {
                                logger.info {
                                    "Vi har allerede beregnet en periode etter denne meldeperioden! Dette blir en omgjøring bak i tid."
                                }
                                add(
                                    Avklaring(
                                        Avklaringkode(
                                            kode = "KorrigeringUtbetaltPeriode",
                                            tittel = "Beregning av meldekort som korrigerer tidligere periode",
                                            beskrivelse = "Behandlingen er korrigering av et tidligere beregnet meldekort",
                                            kanAvbrytes = false,
                                        ),
                                    ),
                                )
                            }
                        }

                        val måAvklareUtdanning = meldekort.dager.any { harMeldtUtdanningUtenGodkjenning(it, forrigeBehandling) }

                        if (måAvklareUtdanning) {
                            val førsteDagMedUtdanning =
                                meldekort.dager
                                    .first { dag -> harMeldtUtdanningUtenGodkjenning(dag, forrigeBehandling) }
                                    .dato
                            opplysninger.leggTil(
                                Faktum(tarUtdanning, true, gyldighetsperiode = Gyldighetsperiode(førsteDagMedUtdanning), kilde = kilde),
                            )

                            add(Avklaring(meldekortMedUtdanning))
                        }

                        val harEksportPerioder = forrigeBehandling.opplysninger.finnAlle(Eksport.oppfyllerVilkårForEksport).any { it.verdi }

                        if (harEksportPerioder) {
                            add(
                                Avklaring(
                                    Avklaringkode(
                                        kode = "MeldekortMedEksport",
                                        tittel = "Meldekort med eksport",
                                        beskrivelse =
                                            """Bruker er innvilget eksport. Sjekk at det beregnes riktig, 
                                            |og ikke får f.eks. trekk ved for sen melding.
                                            """.trimMargin(),
                                        kanAvbrytes = false,
                                        kanKvitteres = true,
                                    ),
                                ),
                            )
                        }
                        if (meldekort.dager.any {
                                val b = runCatching { forrigeBehandling.opplysninger().finnOpplysning(antallBarn, it.dato) }
                                b.isFailure
                            }
                        ) {
                            /*add(
                                Avklaring(
                                    Avklaringkode(
                                        "Mangler barn",
                                        tittel = "Beregning av meldekort",
                                        beskrivelse = "Meldekortet inneholder dager som faller hvor det er registrert barn i tidligere",
                                    ),
                                ),
                            )*/
                        }
                    },
            ).apply {
                this.opplysninger.leggTil(
                    Faktum(
                        Beregning.meldeperiode,
                        Periode(meldekort.fom, meldekort.tom),
                        Gyldighetsperiode(meldekort.fom, meldekort.tom),
                        kilde = saksbehandlerKilde,
                    ),
                )

                val førsteDagMedUtdanning =
                    meldekort.dager
                        .firstOrNull { dag -> dag.aktiviteter.any { it.type == AktivitetType.Utdanning } }
                        ?.dato
                if (førsteDagMedUtdanning != null) {
                    this.opplysninger.leggTil(
                        Faktum(tarUtdanning, true, gyldighetsperiode = Gyldighetsperiode(førsteDagMedUtdanning), kilde = kilde),
                    )
                }

                val meldekortOpplysninger = meldekort.tilOpplysninger(kilde)
                meldekortOpplysninger.forEach { this.opplysninger.leggTil(it) }
            }

        return Opprettet(behandling)
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        internal val meldekortMedUtdanning =
            Avklaringkode(
                kode = "MeldekortMedUtdanning",
                tittel = "Meldekort med utdanning eller tiltak",
                beskrivelse =
                    """Bruker har krysset av for utdanning eller tiltak på meldekortet. Må vurderes manuelt. 
                    |Husk å sjekke om det er godkjent arbeidsmarkedstiltak i Arena.
                    """.trimMargin(),
                kanAvbrytes = false,
                kanKvitteres = true,
            )
    }

    private fun harMeldtUtdanningUtenGodkjenning(
        dag: Dag,
        forrigeBehandling: Behandling,
    ): Boolean {
        val harMeldtUtdanning = dag.aktiviteter.any { it.type == AktivitetType.Utdanning }
        val harGodkjentUtdanning =
            forrigeBehandling
                .opplysninger()
                .forDato(dag.dato)
                .finnNullableOpplysning(godkjentUnntakForUtdanning)
                ?.verdi
                ?: false
        return harMeldtUtdanning && !harGodkjentUtdanning
    }
}
