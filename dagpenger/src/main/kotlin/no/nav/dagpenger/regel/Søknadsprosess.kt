package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.KravPåDagpenger.minsteinntektEllerVerneplikt
import no.nav.dagpenger.regel.Permittering.oppfyllerKravetTilPermittering
import no.nav.dagpenger.regel.ReellArbeidssøker.oppyllerKravTilRegistrertArbeidssøker
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting

class Søknadsprosess : Forretningsprosess {
    override val regelverk = RegelverkDagpenger

    override fun regelsett() = regelverk.regelsett

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> {
        val ønsketResultat =
            mutableListOf<Opplysningstype<*>>(
                Rettighetstype.rettighetstype,
                oppyllerKravTilRegistrertArbeidssøker,
            )

        // Sjekk krav til alder
        ønsketResultat.add(Alderskrav.kravTilAlder)

        if (opplysninger.mangler(Alderskrav.kravTilAlder) || !opplysninger.oppfyller(Alderskrav.kravTilAlder)) {
            return ønsketResultat
        }

        // Sjekk krav til minste arbeidsinntekt eller verneplikt
        ønsketResultat.add(minsteinntektEllerVerneplikt)

        if (opplysninger.mangler(minsteinntektEllerVerneplikt)) {
            return ønsketResultat
        }

        val alderskravOppfylt = opplysninger.oppfyller(Alderskrav.kravTilAlder)
        val minsteinntektOppfylt = opplysninger.oppfyller(minsteinntektEllerVerneplikt)

        // Om krav til alder eller arbeidsinntekt ikke er oppfylt er det ingen grunn til å fortsette, men vi må fastsette hvilke tillegskrav Arena trenger.
        if (!alderskravOppfylt || !minsteinntektOppfylt) {
            ønsketResultat.addAll(
                listOf(
                    ReellArbeidssøker.kravTilArbeidssøker,
                ),
            )
            return ønsketResultat
        }

        val vilkår =
            listOf(
                Alderskrav.kravTilAlder,
                FulleYtelser.ikkeFulleYtelser,
                oppyllerKravTilRegistrertArbeidssøker,
                minsteinntektEllerVerneplikt,
                Opphold.oppfyllerKravet,
                ReellArbeidssøker.kravTilArbeidssøker,
                StreikOgLockout.ikkeStreikEllerLockout,
                TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntektOgArbeidstid,
                Utdanning.kravTilUtdanning,
                Utestengning.oppfyllerKravetTilIkkeUtestengt,
//                oppfyllerKravetTilPermittering,
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
            ønsketResultat.addAll(VernepliktFastsetting.ønsketResultat)
            ønsketResultat.add(oppfyllerKravetTilPermittering)
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
