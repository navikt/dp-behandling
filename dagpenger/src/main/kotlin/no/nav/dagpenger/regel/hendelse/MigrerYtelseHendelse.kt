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
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.prosess.Migreringsprosess
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.Egenandel
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Eksplisitt kontrakt for en ytelse som flyttes over fra et annet system. */
data class MigrertYtelse(
    val innvilgetFraOgMed: LocalDate,
    val innvilgetTilOgMed: LocalDate?,
    val innvilgedeStønadsuker: Int,
    val gjenståendeStønadsdager: Int,
    val dagsats: BigDecimal,
    val antallBarn: Int,
    val vanligArbeidstidPerUke: Double,
    val grunnlag: BigDecimal,
    val egenandel: BigDecimal = BigDecimal.ZERO,
    val kravTilArbeidstidsreduksjon: Double = 50.0,
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
    opprettetAv: Aktør? = null,
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
                    // Meldekort sjekker denne datoen
                    Tilleggsopplysning(Alderskrav.sisteDagIMåned, LocalDate.now().plusDays(365)),
                    // Uten denne lages det avklaring
                    Tilleggsopplysning(Eksport.oppfyllerVilkårForEksport, false),
                    Tilleggsopplysning(Søknadstidspunkt.prøvingsdato, ytelse.innvilgetFraOgMed),
                    Tilleggsopplysning(Dagpengeperiode.ordinærPeriode, ytelse.innvilgedeStønadsuker),
                    Tilleggsopplysning(Dagpengeperiode.antallStønadsdager, ytelse.gjenståendeStønadsdager),
                    Tilleggsopplysning(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg, Beløp(ytelse.dagsats)),
                    Tilleggsopplysning(SamordingUtenforFolketrygden.dagsatsSamordnetUtenforFolketrygden, Beløp(ytelse.dagsats)),
                    Tilleggsopplysning(DagpengenesStørrelse.antallBarn, ytelse.antallBarn),
                    Tilleggsopplysning(Vanligarbeidstid.fastsattVanligArbeidstid, ytelse.vanligArbeidstidPerUke),
                    Tilleggsopplysning(SamordingUtenforFolketrygden.andreYtelser, false),
                    Tilleggsopplysning(Samordning.skalSamordnes, false),
                    Tilleggsopplysning(Samordning.samordnetDagsats, Beløp(ytelse.dagsats)),
                    Tilleggsopplysning(Samordning.utfallEtterSamordning, true),
//                    Tilleggsopplysning(Dagpengegrunnlag.grunnlag, Beløp(ytelse.grunnlag)),
                    Tilleggsopplysning(Egenandel.egenandel, Beløp(ytelse.egenandel)),
                    Tilleggsopplysning(TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon, ytelse.kravTilArbeidstidsreduksjon),
                    Tilleggsopplysning(DagpengenesStørrelse.ukessats, Beløp(ytelse.dagsats) * 7),
                    Tilleggsopplysning(DagpengenesStørrelse.ukesatsMedBarnetillegg, Beløp(ytelse.dagsats) * 7),
                    Tilleggsopplysning(DagpengenesStørrelse.harSamordnet, false),
                    Tilleggsopplysning(DagpengenesStørrelse.barnSomGirTillegg, BarnListe(null, emptyList())),
                ),
            )
        }.map { it.faktum(ytelse.innvilgetPeriode, kilde) }

    private companion object {
        val eksplisitteTyper =
            listOf(
                Søknadstidspunkt.prøvingsdato,
                Dagpengeperiode.ordinærPeriode,
                Dagpengeperiode.antallStønadsdager,
                DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg,
                DagpengenesStørrelse.antallBarn,
                Vanligarbeidstid.fastsattVanligArbeidstid,
//                Dagpengegrunnlag.grunnlag,
                Egenandel.egenandel,
                TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon,
            )
    }
}
