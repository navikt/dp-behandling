package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Regelverkstype
import no.nav.dagpenger.opplysning.Rettighet
import no.nav.dagpenger.opplysning.Utbetaling
import no.nav.dagpenger.opplysning.Vedtak
import no.nav.dagpenger.opplysning.Vilkår
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.fastsetting.PermitteringFraFiskeindustrienFastsetting
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting
import java.time.LocalDate
import java.util.UUID

val RegelverkDagpenger =
    Regelverk(
        Alderskrav.regelsett,
        Vanligarbeidstid.regelsett,
        Dagpengegrunnlag.regelsett,
        DagpengenesStørrelse.regelsett,
        Dagpengeperiode.regelsett,
        Egenandel.regelsett,
        FulleYtelser.regelsett,
        Meldeplikt.regelsett,
        Minsteinntekt.regelsett,
        Opphold.regelsett,
        Opptjeningstid.regelsett,
        ReellArbeidssøker.regelsett,
        RegistrertArbeidssøker.regelsett,
        Rettighetstype.regelsett,
        SamordingUtenforFolketrygden.regelsett,
        Samordning.regelsett,
        StreikOgLockout.regelsett,
        Søknadstidspunkt.regelsett,
        TapAvArbeidsinntektOgArbeidstid.regelsett,
        Utdanning.regelsett,
        Utestengning.regelsett,
        Verneplikt.regelsett,
        VernepliktFastsetting.regelsett,
        Permittering.regelsett,
        PermitteringFastsetting.regelsett,
        PermitteringFraFiskeindustrien.regelsett,
        PermitteringFraFiskeindustrienFastsetting.regelsett,
    )

fun oppfyllerKravetTilMinsteinntektEllerVerneplikt(opplysninger: LesbarOpplysninger): Boolean =
    opplysninger.erSann(minsteinntekt) || opplysninger.erSann(oppfyllerKravetTilVerneplikt)

fun kravPåDagpenger(opplysninger: LesbarOpplysninger): Boolean =
    RegelverkDagpenger.regelsett
        .filter { it.type == RegelsettType.Vilkår }
        .filter { it.påvirkerResultat(opplysninger) }
        .flatMap { it.utfall }
        .all { opplysninger.erSann(it) }

// Vedtaksdefinisjon for dagpenger
data class FastsettelserForDagpenger(
    val fastsattVanligArbeidstid: Fastsatt.FastsattVanligArbeidstid? = null,
    val sats: Fastsatt.Sats? = null,
    val samordning: List<Samordning> = emptyList(),
    val kvoter: List<Kvote> = emptyList(),
) : Regelverkstype

class Dagpenger(
    override val løpende: Boolean,
) : Rettighet

data class DagpengeVedtak(
    override val vedtakId: UUID,
    override val vedtaksdato: LocalDate,
    override val virkningsdato: LocalDate,
    override val vilkår: List<Vilkår>,
    override val fastsatt: FastsettelserForDagpenger,
    override val utbetalinger: List<Utbetaling>,
) : Vedtak<FastsettelserForDagpenger> {
    override val utfall = vilkår.all { it.status }

    override fun blurp(block: (Rettighet) -> Unit) {
        block(Dagpenger(utfall))
    }
}

sealed class Fastsatt(
    val utfall: Boolean,
    val fastsattVanligArbeidstid: FastsattVanligArbeidstid,
    val samordning: List<SamordningYtelse>,
) {
    data class FastsattVanligArbeidstid(
        val fastsattVanligArbeidstidPerUke: Double,
        val nyArbeidstidPerUke: Double,
    )

    data class Sats(
        val dagsatsMedBarnetillegg: Int,
        val barn: List<Barn>,
    ) {
        data class Barn(
            val fødseldato: LocalDate,
            val girTillegg: Boolean,
        )
    }
}

class Innvilgelse(
    val grunnlag: Int,
    fastsattVanligArbeidstid: FastsattVanligArbeidstid,
    val sats: Sats,
    samordning: List<SamordningYtelse>,
    val kvoter: List<Kvote>,
) : Fastsatt(true, fastsattVanligArbeidstid, samordning)

class Avslag(
    fastsattVanligArbeidstid: FastsattVanligArbeidstid,
    samordning: List<SamordningYtelse>,
) : Fastsatt(false, fastsattVanligArbeidstid, samordning)

data class SamordningYtelse(
    val ytelse: String,
    val beløp: Int,
)

data class Kvote(
    val navn: String,
    val type: String,
    val verdi: Int,
)
