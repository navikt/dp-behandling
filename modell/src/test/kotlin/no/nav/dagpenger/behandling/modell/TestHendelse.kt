package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TestHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    søknadId: UUID,
    gjelderDato: LocalDate = LocalDate.now(),
    opprettet: LocalDateTime = LocalDateTime.now(),
) : StartHendelse(
        meldingsreferanseId,
        ident,
        SøknadId(søknadId),
        gjelderDato,
        opprettet,
    ) {
    private val opplysningstypeBehov = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "trengerDenne")
    private val opplysningstype = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "opplysning")
    override val forretningsprosess: Forretningsprosess
        get() =
            object : Forretningsprosess(Regelverk(regelsett)) {
                override fun regelkjøring(opplysninger: Opplysninger) =
                    Regelkjøring(
                        skjedde,
                        opplysninger,
                        regelsett,
                    )

                override fun kontrollpunkter(): List<IKontrollpunkt> = emptyList()

                override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
                    TODO("Not yet implemented")
                }

                override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
                    TODO("Not yet implemented")
                }

                override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> {
                    TODO("Not yet implemented")
                }
            }

    private val regelsett =
        vilkår("test") {
            regel(opplysningstypeBehov) { innhentes }
            regel(opplysningstype) { enAv(opplysningstypeBehov) }
        }

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        TODO("Not yet implemented")
    }
}
