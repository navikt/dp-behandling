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
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Systemaktør
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.regel.prosess.Migreringsprosess
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Eksplisitt kontrakt for en ytelse som flyttes over fra et annet system. */
data class MigrertYtelse(
    val innvilgetFraOgMed: LocalDate,
    val innvilgetTilOgMed: LocalDate?,
    val sakId: Int,
) {
    val innvilgetPeriode get() = innvilgetTilOgMed?.let { Gyldighetsperiode(innvilgetFraOgMed, it) } ?: Gyldighetsperiode(innvilgetFraOgMed)
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
) : StartHendelse(meldingsreferanseId, ident, eksternId, ytelse.innvilgetFraOgMed, opprettet, opprettetAv) {
    override val forretningsprosess: Forretningsprosess = Migreringsprosess()

    private val kilde = Systemkilde(meldingsreferanseId, opprettet)

    init {
        val typer = eksplisitteTyper + tilleggsopplysninger.map { it.opplysningstype }
        val duplikater =
            typer
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        require(duplikater.isEmpty()) { "Opplysninger kan bare oppgis én gang: ${duplikater.map { it.behovId }}" }
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
                faktaFraKontrakt().forEach { opplysninger.leggTil(it) }
                tilleggsopplysninger.forEach { opplysninger.leggTil(it.faktum(ytelse.innvilgetPeriode, kilde)) }
            },
        )
    }

    private fun faktaFraKontrakt() =
        buildList {
            forretningsprosess.regelverk.vilkårsopplysninger.forEach {
                add(Tilleggsopplysning(it, true))
                println("Legger til vilkår ${it.behovId} som oppfylt")
            }

            addAll(
                listOf(
                    // verdier fra kontrakten
                    Tilleggsopplysning(Søknadstidspunkt.prøvingsdato, ytelse.innvilgetFraOgMed),
                    Tilleggsopplysning(SøknadInnsendtHendelse.fagsakIdOpplysningstype, ytelse.sakId),
                    // Defaultverdier vi setter selv
                    Tilleggsopplysning(Eksport.oppfyllerVilkårForEksport, false),
                    // finne ut hvordan vi skal lage barnelisten
                    Tilleggsopplysning(DagpengenesStørrelse.barnSomGirTillegg, BarnListe(null, emptyList())),
                ),
            )
        }.map { it.faktum(ytelse.innvilgetPeriode, kilde) }

    private companion object {
        val eksplisitteTyper =
            listOf(
                Søknadstidspunkt.prøvingsdato,
                SøknadInnsendtHendelse.fagsakIdOpplysningstype,
                Eksport.oppfyllerVilkårForEksport,
            )
    }
}
