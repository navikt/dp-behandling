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
import no.nav.dagpenger.regel.Meldeplikt.oppfyllerMeldeplikt
import no.nav.dagpenger.regel.RegistrertArbeidssøker.registrertArbeidssøker
import no.nav.dagpenger.regel.Stansprosess
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AvsluttetArbeidssøkerperiode(
    val arbeidssøkerperiodeId: ArbeidssøkerperiodeId,
    val fastsattMeldingsdag: LocalDate,
    val avsluttetTidspunkt: LocalDateTime,
    val mottattTidspunkt: LocalDateTime,
    val fristBrutt: Boolean,
    val manueltAvregistrert: Boolean,
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
        skjedde = avsluttetArbeidssøkerperiode.avsluttetTidspunkt.toLocalDate(),
        opprettet = opprettet,
    ) {
    override val forretningsprosess = Stansprosess()

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
                    if (avsluttetArbeidssøkerperiode.manueltAvregistrert) {
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

                    // TODO: Ta bort denne når vi mener disse kan gå automatisk. Husk testene i ArbeidssøkerTest
                    add(
                        Avklaring(
                            Avklaringkode(
                                kode = "UtmeldtArbeidssøker",
                                tittel = "Bruker har blitt utmeldt av arbeidssøkerregisteret",
                                beskrivelse = "Bruker er ikke lenger arbeidssøker. Ta stilling til om forslaget til stans er riktig.",
                                kanAvbrytes = false,
                            ),
                        ),
                    )
                },
        ).apply {
            // Marker bruker som ikke lenger registrert fra og med avsluttetTidspunkt.
            opplysninger.leggTil(
                Faktum(
                    registrertArbeidssøker,
                    false,
                    Gyldighetsperiode(fraOgMed = avsluttetArbeidssøkerperiode.avsluttetTidspunkt.toLocalDate()),
                    kilde = kilde,
                ),
            )

            // Om meldekort sendes inn etter 21-dagers fristen skal også få stans på § 4-8
            if (avsluttetArbeidssøkerperiode.fristBrutt) {
                val meldingsdag = avsluttetArbeidssøkerperiode.fastsattMeldingsdag
                opplysninger.leggTil(
                    Faktum(
                        oppfyllerMeldeplikt,
                        false,
                        Gyldighetsperiode(fraOgMed = meldingsdag),
                        kilde = kilde,
                    ),
                )
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
