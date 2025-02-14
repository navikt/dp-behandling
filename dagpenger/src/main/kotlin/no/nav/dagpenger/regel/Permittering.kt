package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Avklaringspunkter.HarOppgittPermittering
import no.nav.dagpenger.regel.OpplysningsTyper.erPermitteringenMidlertidigId
import no.nav.dagpenger.regel.OpplysningsTyper.godkjentPermitteringsårsakId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravetTilPermitteringId
import no.nav.dagpenger.regel.Rettighetstype.erPermittert
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

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

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 7, "Dagpenger til permitterte", "Permittering")) {

            skalVurderes { it.erSann(kravTilAlder) }

            regel(godkjentPermitteringsårsak) { oppslag(prøvingsdato) { true } }
            regel(erPermitteringenMidlertidig) { oppslag(prøvingsdato) { true } }

            utfall(oppfyllerKravetTilPermittering) {
                alle(
                    erPermittert,
                    godkjentPermitteringsårsak,
                    erPermitteringenMidlertidig,
                )
            }

            påvirkerResultat(erPermittert())

            avklaring(HarOppgittPermittering)
        }

    private fun erPermittert(): (LesbarOpplysninger) -> Boolean =
        {
            it.erSann(
                erPermittert,
            )
        }

    val PermitteringKontroll =
        Kontrollpunkt(HarOppgittPermittering) {
            it.har(erPermittert) && it.finnOpplysning(erPermittert).verdi
        }
}
