package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.EksternId
import no.nav.dagpenger.behandling.modell.hendelser.Hendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.Søknadsprosess
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OpprettBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    eksternId: EksternId<*>,
    gjelderDato: LocalDate,
    private val begrunnelse: String? = null,
    opprettet: LocalDateTime,
) : StartHendelse(meldingsreferanseId, ident, eksternId, gjelderDato, opprettet) {
    override val forretningsprosess = Søknadsprosess()

    override val regelverk: Regelverk
        get() = forretningsprosess.regelverk

    override fun regelkjøring(opplysninger: Opplysninger) =
        Regelkjøring(
            virkningsdato(opplysninger),
            opplysninger,
            this,
            opplysningerGyldigPåPrøvingsdato,
        )

    private val opplysningerGyldigPåPrøvingsdato: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger =
        { forDato(virkningsdato(this)) }

    override fun ønsketResultat(opplysninger: LesbarOpplysninger) = forretningsprosess.ønsketResultat(opplysninger)

    override fun kontrollpunkter() = forretningsprosess.kontrollpunkter()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = forretningsprosess.kreverTotrinnskontroll(opplysninger)

    override fun virkningsdato(opplysninger: LesbarOpplysninger) = skjedde

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ) = Behandling(
        basertPå = listOfNotNull(forrigeBehandling),
        behandler =
            Hendelse(
                meldingsreferanseId = meldingsreferanseId,
                type = type,
                ident = ident,
                eksternId = eksternId,
                skjedde = skjedde,
                opprettet = opprettet,
                forretningsprosess = forretningsprosess,
            ),
        opplysninger =
            listOf(
                Faktum(
                    hendelseTypeOpplysningstype,
                    type,
                    gyldighetsperiode = Gyldighetsperiode(fom = skjedde),
                    kilde = Systemkilde(meldingsreferanseId, opprettet),
                ),
            ),
        avklaringer =
            listOf(
                Avklaring(
                    Avklaringkode(
                        kode = "ManuellBehandling",
                        tittel = "Manuell behandling",
                        beskrivelse = begrunnelse ?: "Behandlingen er opprettet manuelt og kan ikke automatisk behandles",
                        kanAvbrytes = false,
                    ),
                ),
            ),
    )
}
