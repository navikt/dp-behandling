package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.dato.finnDagensDato
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.fraOgMed
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.OpplysningsTyper.DagensDatoId
import no.nav.dagpenger.regel.OpplysningsTyper.EttBeregnetVirkningstidspunktId
import no.nav.dagpenger.regel.OpplysningsTyper.KravPåDagpengerId
import no.nav.dagpenger.regel.OpplysningsTyper.MinsteinntektEllerVernepliktId
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt

object KravPåDagpenger {
    val kravPåDagpenger = boolsk(KravPåDagpengerId, "Krav på dagpenger")
    val minsteinntektEllerVerneplikt = boolsk(MinsteinntektEllerVernepliktId, "Oppfyller kravet til minsteinntekt eller verneplikt")

    val virkningstidspunkt = dato(EttBeregnetVirkningstidspunktId, "EttBeregnetVirkningstidspunkt")

    val dagensDato = dato(DagensDatoId, "Dagens dato")

    val regelsett =
        vilkår("Krav på dagpenger") {
            skalVurderes { false }

            regel(minsteinntektEllerVerneplikt) { enAv(minsteinntekt, oppfyllerKravetTilVerneplikt) }

            regel(dagensDato) { finnDagensDato }
            regel(virkningstidspunkt) { fraOgMed(kravPåDagpenger) }

            utfall(kravPåDagpenger) {
                alle(
                    Alderskrav.kravTilAlder,
                    FulleYtelser.ikkeFulleYtelser,
                    ReellArbeidssøker.oppyllerKravTilRegistrertArbeidssøker,
                    minsteinntektEllerVerneplikt,
                    Opphold.oppfyllerKravet,
                    ReellArbeidssøker.kravTilArbeidssøker,
                    Rettighetstype.rettighetstype,
                    Samordning.utfallEtterSamordning,
                    StreikOgLockout.ikkeStreikEllerLockout,
                    TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntektOgArbeidstid,
                    Utdanning.kravTilUtdanning,
                    Utestengning.oppfyllerKravetTilIkkeUtestengt,
                )
            }
            påvirkerResultat { false }
        }

    val VirkningstidspunktForLangtFramITid =
        Kontrollpunkt(Avklaringspunkter.VirkningstidspunktForLangtFramITid) {
            it.har(virkningstidspunkt) &&
                it.har(dagensDato) &&
                it.finnOpplysning(virkningstidspunkt).verdi.isAfter(
                    it.finnOpplysning(dagensDato).verdi.plusDays(14),
                )
        }
}
