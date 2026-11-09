package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.hendelser.EksternId
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.Opprettet
import no.nav.dagpenger.opplysning.Aktør
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Systemaktør
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.prosess.Migreringsprosess
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Eksplisitt kontrakt for en ytelse som flyttes over fra et annet system. */
data class MigrertYtelse(
    val migrereringTidspunkt: LocalDate,
    val innvilgetTilOgMed: LocalDate?,
    val sakId: Int,
) {
    val innvilgetPeriode get() =
        innvilgetTilOgMed?.let { Gyldighetsperiode(migrereringTidspunkt, it) }
            ?: Gyldighetsperiode(migrereringTidspunkt)
}

/**
 * Opplysning utenfor den eksplisitte kontrakten. Bruker ytelsens periode hvis opplysningen mangler egen periode.
 */
class Tilleggsopplysning<T : Any>(
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode? = null,
) {
    fun faktum(
        standardGyldighetsperiode: Gyldighetsperiode,
        kilde: Kilde,
    ) = Faktum(opplysningstype, verdi, gyldighetsperiode ?: standardGyldighetsperiode, kilde = kilde)
}

class MigrerYtelseHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    eksternId: EksternId<*>,
    opprettet: LocalDateTime,
    private val ytelse: MigrertYtelse,
    private val tilleggsopplysninger: List<Tilleggsopplysning<*>> = emptyList(),
    opprettetAv: Aktør = Systemaktør.dpSak,
) : StartHendelse(meldingsreferanseId, ident, eksternId, ytelse.migrereringTidspunkt, opprettet, opprettetAv) {
    override val forretningsprosess: Forretningsprosess = Migreringsprosess()

    private val kilde = Systemkilde(meldingsreferanseId, opprettet)

    // Vi har ingen søknad. Vi lager en random uuid. Søknadsfakta må komme fra migreringsmeldingen.
    private val søknadId = UUID.randomUUID().toString()

    init {
        val eksplisitteDuplikater =
            tilleggsopplysninger
                .map { it.opplysningstype }
                .filter { it in eksplisitteTyper }
                .toSet()
        require(eksplisitteDuplikater.isEmpty()) {
            "Opplysninger kan bare oppgis én gang: ${eksplisitteDuplikater.map { it.id.uuid }}"
        }
        val overlappendePerioder =
            tilleggsopplysninger
                .groupBy { it.opplysningstype }
                .filterValues { opplysninger ->
                    val perioder = opplysninger.map { it.gyldighetsperiode ?: ytelse.innvilgetPeriode }
                    perioder.size > 1 &&
                        perioder
                            .sortedBy { it.fraOgMed }
                            .zipWithNext()
                            .any { (a, b) -> a.overlapper(b) }
                }.keys
        require(overlappendePerioder.isEmpty()) {
            "Opplysninger kan ikke ha overlappende gyldighetsperioder: ${overlappendePerioder.map { it.id.uuid }}"
        }
        val vurderinger = tilleggsopplysninger.map { it.opplysningstype }.toSet().intersect(vilkårsvurderinger)
        require(vurderinger.isEmpty()) {
            "Vilkårsvurderinger kan ikke oppgis ved migrering: ${vurderinger.map { it.id.uuid }}"
        }
    }

    internal fun lagFaktagrunnlag(): Opplysninger =
        Opplysninger().also {
            it.leggTil(hendelseTypeFaktum)
            leggTilFakta(it)
        }

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat {
        /*if (forrigeBehandling != null) {
            return IkkeOpprettet("Hendelse av type $type kan ikke migrere en ytelse for en person som allerede har en behandling")
        }*/

        return Opprettet(
            opprettBehandling(
                basertPå = null,
                avklaringer =
                    listOf(
                        Avklaring(
                            Avklaringkode(
                                kode = "MigrertYtelse",
                                tittel = "Migrert ytelse",
                                beskrivelse = "Ytelsen er migrert fra et annet system og må kontrolleres",
                                kanAvbrytes = false,
                            ),
                        ),
                    ),
            ).apply {
                leggTilFakta(opplysninger)
            },
        )
    }

    private fun leggTilFakta(opplysninger: Opplysninger) {
        val standardfakta =
            listOf(
                Tilleggsopplysning(Søknadstidspunkt.prøvingsdato, ytelse.migrereringTidspunkt),
                Tilleggsopplysning(Søknadstidspunkt.søknadIdOpplysningstype, søknadId),
                Tilleggsopplysning(SøknadInnsendtHendelse.fagsakIdOpplysningstype, ytelse.sakId),
                Tilleggsopplysning(Rettighetstype.skalGjenopptakVurderes, false),
            )
        (standardfakta + tilleggsopplysninger)
            .map { it.faktum(ytelse.innvilgetPeriode, kilde) }
            .forEach { opplysninger.leggTil(it) }
    }

    private companion object {
        private val vilkårsvurderinger = RegelverkDagpenger.vilkårsopplysninger.toSet()

        val eksplisitteTyper =
            listOf(
                Søknadstidspunkt.prøvingsdato,
                Søknadstidspunkt.søknadIdOpplysningstype,
                SøknadInnsendtHendelse.fagsakIdOpplysningstype,
                Rettighetstype.skalGjenopptakVurderes,
            )
    }
}
