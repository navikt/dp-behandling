package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.EksternId
import no.nav.dagpenger.behandling.modell.hendelser.Hendelse
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.Omgjøringsprosess
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OmgjøringHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    eksternId: EksternId<*>,
    gjelderDato: LocalDate,
    opprettet: LocalDateTime,
    private val meldekortkorrigeringerSupplier: (originale: List<MeldekortId>) -> List<Meldekort>,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = eksternId,
        skjedde = gjelderDato,
        opprettet = opprettet,
    ) {
    override val forretningsprosess = Omgjøringsprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        requireNotNull(forrigeBehandling) { "Omgjøring krever en tidligere behandling å basere seg på" }

        val kilde = Systemkilde(meldingsreferanseId, opprettet)

        val beregnedeMeldekort =
            buildList {
                traverserBehandlingskjede(forrigeBehandling) {
                    add(it.behandler.eksternId)
                }
            }.filterIsInstance<MeldekortId>()

        val meldekortkorrigeringer = meldekortkorrigeringerSupplier(beregnedeMeldekort)

        return Behandling(
            basertPå = forrigeBehandling,
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
            opplysninger = emptyList(),
            avklaringer =
                if (meldekortkorrigeringer.isEmpty()) {
                    emptyList()
                } else {
                    listOf(
                        Avklaring(
                            Avklaringkode(
                                kode = "KorrigertMeldekortBehandling",
                                tittel = "Beregning av korrigert meldekort",
                                beskrivelse = "Behandlingen er korrigering av et tidligere meldekort og kan ikke automatisk behandles",
                                kanAvbrytes = false,
                            ),
                        ),
                    )
                },
        ).also { nyBehandling ->
            nyBehandling.opplysninger.leggTil(
                Faktum(
                    hendelseTypeOpplysningstype,
                    type,
                    gyldighetsperiode = Gyldighetsperiode.kun(skjedde),
                    kilde = kilde,
                ),
            )
            val meldekortOpplysninger = meldekortkorrigeringer.flatMap { it.tilOpplysninger(kilde) }
            meldekortOpplysninger.forEach { nyBehandling.opplysninger.leggTil(it) }
        }
    }
}

private fun traverserBehandlingskjede(
    startbehandling: Behandling,
    gjørNoe: (Behandling) -> Unit,
) {
    var pointer: Behandling? = startbehandling
    while (pointer != null) {
        gjørNoe(pointer)
        pointer = pointer.basertPå
    }
}
