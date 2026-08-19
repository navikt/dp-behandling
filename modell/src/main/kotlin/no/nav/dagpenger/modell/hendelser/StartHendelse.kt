package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.Utledning
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed class StartHendelseResultat {
    data class Opprettet(
        val behandling: Behandling,
    ) : StartHendelseResultat()

    data class OppdaterBehandling(
        val årsak: String,
    ) : StartHendelseResultat()

    data class IkkeOpprettet(
        val årsak: String,
    ) : StartHendelseResultat()
}

// Baseklasse for alle hendelser som kan påvirke dagpengene til en person og må behandles
abstract class StartHendelse(
    val meldingsreferanseId: UUID,
    val ident: String,
    val eksternId: EksternId<*>,
    val skjedde: LocalDate,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet) {
    open val type: String = this.javaClass.simpleName

    fun erSammeType(hendelse: StartHendelse): Boolean = this.type == hendelse.type

    override fun kontekstMap() =
        mapOf(
            "gjelderDato" to skjedde.toString(),
        ) + eksternId.kontekstMap()

    abstract val forretningsprosess: Forretningsprosess

    abstract fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat

    open val opplysninger: Opplysninger = Opplysninger()

    /** Faktumet for hvilken hendelsestype som opprettet behandlingen. */
    private val hendelseTypeFaktum: Faktum<String> by lazy {
        Faktum(
            hendelseTypeOpplysningstype,
            type,
            gyldighetsperiode = Gyldighetsperiode.kun(skjedde),
            kilde = Systemkilde(meldingsreferanseId, opprettet),
        )
    }

    /**
     * Gir en [Utledning] som gjør at et faktum nullstilles (markeres utdatert, se `markerUtledningerSomUtdatert`)
     * når en senere hendelse overtar hendelsetypen. Kun trygt å bruke i create-path (`opprettBehandling`), siden
     * hendelseTypeFaktum kun er garantert lagt til på behandlingen når hendelsen faktisk oppretter den – ikke ved
     * merge inn i en annen, pågående behandling.
     */
    protected fun nullstillesVedNyHendelse(regel: String = this::class.java.simpleName) = Utledning(regel, listOf(hendelseTypeFaktum))

    /**
     * Oppretter en ny [Behandling] for denne hendelsen og legger til [hendelseTypeFaktum].
     * Konkrete [StartHendelse]-implementasjoner skal bruke denne fremfor å konstruere [Behandling] selv.
     * Opplysninger som avhenger av `forrigeBehandling` (kun tilgjengelig i [behandling]), eller andre opplysninger
     * som er spesifikke for denne hendelsen, legges til via [byggOpplysninger].
     */
    protected fun opprettBehandling(
        basertPå: Behandling?,
        avklaringer: List<Avklaring> = emptyList(),
        byggOpplysninger: Opplysninger.() -> Unit = {},
    ): Behandling =
        Behandling(
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
            basertPå = basertPå,
            avklaringer = avklaringer,
        ).apply {
            opplysninger.leggTil(hendelseTypeFaktum)
            opplysninger.byggOpplysninger()
        }
}
