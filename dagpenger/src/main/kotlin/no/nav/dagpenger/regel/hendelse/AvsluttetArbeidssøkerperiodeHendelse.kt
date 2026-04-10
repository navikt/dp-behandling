package no.nav.dagpenger.regel.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.ArbeidssøkerperiodeId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Manuellprosess
import no.nav.dagpenger.regel.RegistrertArbeidssøker
import no.nav.dagpenger.regel.RegistrertArbeidssøker.registrertArbeidssøker
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AvsluttetArbeidssøkerperiode(
    val arbeidssøkerperiodeId: ArbeidssøkerperiodeId,
    val fastsattMeldingsdag: LocalDate? = null,
    val avsluttetTidspunkt: LocalDateTime,
    val mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
)

class AvsluttetArbeidssøkerperiodeHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    opprettet: LocalDateTime,
    private val avsluttetArbeidssøkerperiode: AvsluttetArbeidssøkerperiode,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = avsluttetArbeidssøkerperiode.arbeidssøkerperiodeId,
        skjedde = avsluttetArbeidssøkerperiode.mottattTidspunkt.toLocalDate(),
        opprettet = opprettet,
    ) {
    override val forretningsprosess = Manuellprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        requireNotNull(forrigeBehandling) { "Må ha en behandling å ta utgangspunkt i" }

        val kilde = Systemkilde(meldingsreferanseId, opprettet)

        return Behandling(
            basertPå = forrigeBehandling,
            behandler = this,
            opplysninger =
                listOf(
                    Faktum(
                        hendelseTypeOpplysningstype,
                        type,
                        Gyldighetsperiode.kun(skjedde),
                        kilde = kilde,
                    ),
                ),
            avklaringer =
                buildList {
                    if (avsluttetArbeidssøkerperiode.fastsattMeldingsdag == null) {
                        add(
                            Avklaring(
                                Avklaringkode(
                                    kode = "ManueltUtmeldt",
                                    tittel = "Bruker har blitt utmeldt av ASR utenfor dagpenger",
                                    beskrivelse =
                                        """Bruker har blitt utmeldt av ASR utenfor dagpenger, og må derfor vurderes manuelt. Sjekk 
                                        |hvordan dette påvirker retten til dagpenger og fra hvilken dato en eventuell stans skal gjelde fra.
                                        """.trimMargin(),
                                    kanAvbrytes = false,
                                ),
                            ),
                        )
                    }
                },
        ).apply {
            // Oppdater status som registrert
            val gjeldendeRegistrering = opplysninger.finnOpplysning(RegistrertArbeidssøker.registrertArbeidssøker)
            opplysninger.leggTil(
                Faktum(
                    RegistrertArbeidssøker.registrertArbeidssøker,
                    gjeldendeRegistrering.verdi,
                    gjeldendeRegistrering.gyldighetsperiode.copy(tilOgMed = avsluttetArbeidssøkerperiode.avsluttetTidspunkt.toLocalDate()),
                    kilde = kilde,
                ),
            )

            // Legg til stansdato (som blir manuelt sjekket når utmelding skjer utenfor dagpenger)
            opplysninger.leggTil(
                Faktum(
                    harLøpendeRett,
                    false,
                    Gyldighetsperiode(
                        fraOgMed =
                            avsluttetArbeidssøkerperiode.fastsattMeldingsdag
                                ?: avsluttetArbeidssøkerperiode.avsluttetTidspunkt.toLocalDate(),
                    ),
                    kilde = kilde,
                ),
            )
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
