package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting

// @todo: Snu avhengighetet  - søknadinnsendtHendelse bør leve i "dagpenger"
val støtterInnvilgelseOpplysningstype = Opplysningstype.somBoolsk("støtterInnvilgelse")

class Søknadsprosess : Forretningsprosess {
    private val regelverk = RegelverkDagpenger

    override fun regelsett() = regelverk.regelsett

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> {
        val ønsketResultat = mutableListOf<Opplysningstype<*>>()

        // Sjekk krav til alder
        ønsketResultat.add(Alderskrav.kravTilAlder)

        // Sjekk krav til minste arbeidsinntekt
        ønsketResultat.add(Minsteinntekt.minsteinntekt)

        if (opplysninger.mangler(Alderskrav.kravTilAlder) || opplysninger.mangler(Minsteinntekt.minsteinntekt)) {
            return ønsketResultat
        }

        val alderskravOppfylt = opplysninger.oppfyller(Alderskrav.kravTilAlder)
        val minsteinntektOppfylt = opplysninger.oppfyller(Minsteinntekt.minsteinntekt)

        val støtterInnvilgelse = opplysninger.oppfyller(støtterInnvilgelseOpplysningstype)

        // Om krav til alder eller arbeidsinntekt ikke er oppfylt er det ingen grunn til å fortsette, men vi må fastsette hvilke tillegskrav Arena trenger.
        if (!alderskravOppfylt || !minsteinntektOppfylt) {
            ønsketResultat.addAll(
                listOf(
                    ReellArbeidssøker.kravTilArbeidssøker,
                    Meldeplikt.registrertPåSøknadstidspunktet,
                    Rettighetstype.rettighetstype,
                ),
            )
            return ønsketResultat
        }
        if (!støtterInnvilgelse) {
            return ønsketResultat
        }

        val vilkår =
            listOf(
                Alderskrav.kravTilAlder,
                FulleYtelser.ikkeFulleYtelser,
                Medlemskap.oppfyllerMedlemskap,
                Meldeplikt.registrertPåSøknadstidspunktet,
                Minsteinntekt.minsteinntekt,
                Opphold.oppfyllerKravet,
                ReellArbeidssøker.kravTilArbeidssøker,
                Rettighetstype.rettighetstype,
                StreikOgLockout.ikkeStreikEllerLockout,
                TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntektOgArbeidstid,
                Utdanning.kravTilUtdanning,
                Utestengning.oppfyllerKravetTilIkkeUtestengt,
            )
        ønsketResultat.addAll(
            vilkår,
        )
        if (opplysninger.mangler(vilkår)) {
            return ønsketResultat
        }

        if (opplysninger.oppfyller(vilkår)) {
            ønsketResultat.addAll(Dagpengegrunnlag.ønsketResultat)
            ønsketResultat.addAll(Egenandel.ønsketResultat)
            ønsketResultat.addAll(DagpengenesStørrelse.ønsketResultat)
            ønsketResultat.addAll(Dagpengeperiode.ønsketResultat)
            ønsketResultat.addAll(Samordning.ønsketResultat)
            ønsketResultat.addAll(SamordingUtenforFolketrygden.ønsketResultat)

            if (opplysninger.oppfyller(Verneplikt.avtjentVerneplikt)) {
                ønsketResultat.addAll(VernepliktFastsetting.ønsketResultat)
            }
        }

        ønsketResultat.add(KravPåDagpenger.kravPåDagpenger)
        return ønsketResultat
    }

    private fun LesbarOpplysninger.mangler(opplysningstype: List<Opplysningstype<Boolean>>): Boolean =
        opplysningstype.all { this.mangler(it) }

    private fun LesbarOpplysninger.oppfyller(opplysningstype: List<Opplysningstype<Boolean>>): Boolean =
        opplysningstype.all { oppfyller(it) }

    private fun LesbarOpplysninger.oppfyller(opplysningstype: Opplysningstype<Boolean>) =
        har(opplysningstype) && finnOpplysning(opplysningstype).verdi
}
