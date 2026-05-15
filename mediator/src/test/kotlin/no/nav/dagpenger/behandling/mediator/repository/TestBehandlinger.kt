package no.nav.dagpenger.behandling.mediator.repository

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosessregister
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Syntetisk test-infrastruktur for repository-tester.
 * Ingen avhengighet til dagpenger- eller ferietillegg-modulene.
 */
internal object TestBehandlinger {
    private val testOpplysningstype = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "test-vilkår")

    private val testRegelsett =
        vilkår("test") {
            regel(testOpplysningstype) { innhentes }
        }

    val testRegelverk = Regelverk(RegelverkType("Test"), regelsett = arrayOf(testRegelsett))

    val testAvklaringskode = Avklaringkode("TestAvklaring", "Test", "Avklaring for testing")

    fun registrerTestProsesser(prosessregister: Prosessregister) {
        prosessregister.registrer(TestProsess())
    }

    fun lagTestHendelse(
        ident: String,
        meldingsreferanseId: UUID = UUIDv7.ny(),
        søknadId: UUID = meldingsreferanseId,
        gjelderDato: LocalDate = LocalDate.now(),
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) = TestStartHendelse(meldingsreferanseId, ident, søknadId, gjelderDato, opprettet)

    fun rehydrerBehandling(
        ident: String = "12345678911",
        tilstand: TilstandType = TilstandType.ForslagTilVedtak,
        avklaringer: List<Avklaring> = emptyList(),
    ): Behandling {
        val hendelse = lagTestHendelse(ident)
        return Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = hendelse,
            gjeldendeOpplysninger = Opplysninger.med(Faktum(testOpplysningstype, true)),
            opprettet = LocalDateTime.now(),
            tilstand = tilstand,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = avklaringer,
        )
    }
}

internal class TestProsess : Forretningsprosess(TestBehandlinger.testRegelverk) {
    override fun regelkjøring(opplysninger: Opplysninger) =
        Regelkjøring(LocalDate.now(), opplysninger, *TestBehandlinger.testRegelverk.regelsett.toTypedArray())

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = LocalDate.now()

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> = emptyList()
}

internal class TestStartHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    søknadId: UUID,
    gjelderDato: LocalDate,
    opprettet: LocalDateTime,
) : StartHendelse(meldingsreferanseId, ident, SøknadId(søknadId), gjelderDato, opprettet) {
    override val forretningsprosess = TestProsess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat = throw UnsupportedOperationException("Brukes bare for rehydrering i tester")
}
