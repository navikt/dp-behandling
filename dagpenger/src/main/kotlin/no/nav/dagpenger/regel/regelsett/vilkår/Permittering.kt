package no.nav.dagpenger.regel.regelsett.vilkår
import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.fraOgMed
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Avklaringspunkter.HarOppgittPermittering
import no.nav.dagpenger.regel.OpplysningsTyper.erPermitteringenMidlertidigId
import no.nav.dagpenger.regel.OpplysningsTyper.godkjentPermitteringsårsakId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravetTilPermitteringFraDatoId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravetTilPermitteringId
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting.innenforFritaksperioden
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalPermitteringVurderes

object Permittering {
    val godkjentPermitteringsårsak =
        boolsk(godkjentPermitteringsårsakId, "Årsaken til permitteringen er godkjent", synlig = erPermittert())
    val erPermitteringenMidlertidig =
        boolsk(
            erPermitteringenMidlertidigId,
            "Permitteringen er midlertidig driftsinnskrenkning eller driftsstans",
            synlig = erPermittert(),
        )
    val oppfyllerKravetTilPermittering =
        boolsk(oppfyllerKravetTilPermitteringId, "Oppfyller kravet til permittering", synlig = erPermittert())

    val oppfyllerKravetTilPermitteringFraDato =
        dato(oppfyllerKravetTilPermitteringFraDatoId, "Dato permitteringen løper fra", synlig = aldriSynlig)

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 7, "Dagpenger til permitterte", "Permittering")) {
            skalVurderes { it.erSann(kravTilAlder) && it.erSann(skalPermitteringVurderes) }

            regel(godkjentPermitteringsårsak) { somUtgangspunkt(true) }
            regel(erPermitteringenMidlertidig) { somUtgangspunkt(true) }

            utfall(oppfyllerKravetTilPermittering) {
                alle(
                    skalPermitteringVurderes,
                    godkjentPermitteringsårsak,
                    erPermitteringenMidlertidig,
                    innenforFritaksperioden,
                )
            }

            regel(oppfyllerKravetTilPermitteringFraDato) { fraOgMed(oppfyllerKravetTilPermittering) }
            ønsketResultat(oppfyllerKravetTilPermitteringFraDato)

            påvirkerResultat { it.erSann(skalPermitteringVurderes) }

            avklaring(HarOppgittPermittering)
        }

    private fun erPermittert(): (LesbarOpplysninger) -> Boolean = { it.erSann(skalPermitteringVurderes) }

    val PermitteringKontroll =
        Kontrollpunkt(HarOppgittPermittering) {
            if (it.har(godkjentPermitteringsårsak) &&
                it.finnOpplysning(godkjentPermitteringsårsak).kilde is Saksbehandlerkilde
            ) {
                // Om unntak allerede er gitt er det ikke nødvendig med en avklaring
                return@Kontrollpunkt false
            }
            it.har(skalPermitteringVurderes) && it.finnOpplysning(skalPermitteringVurderes).verdi
        }
}
