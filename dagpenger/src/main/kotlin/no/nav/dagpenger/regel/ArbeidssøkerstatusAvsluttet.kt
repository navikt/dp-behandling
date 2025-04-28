package no.nav.dagpenger.regel

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.ArbeidssøkerPeriodeId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.RegistrertArbeidssøker.registrertArbeidssøker
import no.nav.dagpenger.regel.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class ArbeidssøkerstatusAvsluttet(
    meldingsreferanseId: UUID,
    ident: String,
    opprettet: LocalDateTime,
    val periodeId: UUID,
    val periode: Periode,
    val avsluttetAv: String? = null,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        opprettet = opprettet,
        skjedde = opprettet.toLocalDate(),
        eksternId = ArbeidssøkerPeriodeId(periodeId),
    ) {
    override val forretningsprosess = Søknadsprosess()

    override val regelverk: Regelverk
        get() = forretningsprosess.regelverk

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring =
        Regelkjøring(
            regelverksdato = skjedde,
            prøvingsdato = periode.tilOgMed.plusDays(1),
            opplysninger = opplysninger,
            forretningsprosess = forretningsprosess,
        )

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling? {
        requireNotNull(forrigeBehandling) { "Må ha en behandling å ta utgangspunkt i" }
        val kilde = Systemkilde(meldingsreferanseId, opprettet)

        if (rettighetstatus.contents().isNotEmpty() && rettighetstatus.get(skjedde).utfall) {
            info("Opprettet behandling for arbeidssøkerstatus avsluttet")
            return Behandling(
                behandler = this,
                basertPå = listOf(forrigeBehandling),
                opplysninger =
                    listOf(
                        Faktum(
                            hendelseTypeOpplysningstype,
                            type,
                            gyldighetsperiode = Gyldighetsperiode(fom = skjedde),
                            kilde = kilde,
                        ),
                    ),
            ).apply {
                opplysninger.leggTil(
                    Faktum(
                        registrertArbeidssøker,
                        false,
                        gyldighetsperiode = Gyldighetsperiode(fom = periode.fraOgMed, tom = periode.tilOgMed),
                        kilde = kilde,
                    ),
                )
            }
        } else {
            return null
        }
    }

    override fun kontrollpunkter(): List<IKontrollpunkt> = emptyList()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean = false

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = skjedde

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> = emptyList()
}
