package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Avklaringspunkter.HarOppgittPermitteringFiskeindustri
import no.nav.dagpenger.regel.OpplysningsTyper.erPermitteringenFraFiskeindustriMidlertidigId
import no.nav.dagpenger.regel.OpplysningsTyper.godkjentÅrsakPermitteringFraFiskindustriId
import no.nav.dagpenger.regel.OpplysningsTyper.kravTilProsentvisTapAvArbeidstidFiskepermitteringId
import no.nav.dagpenger.regel.OpplysningsTyper.oppfyllerKravetTilPermitteringFiskeindustriId
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.erPermitteringenFraFiskeindustriMidlertidig
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.godkjentÅrsakPermitteringFraFiskindustri
import no.nav.dagpenger.regel.Rettighetstype.permitteringFiskeforedling

object PermitteringFraFiskeindustrien {
    val godkjentÅrsakPermitteringFraFiskindustri =
        boolsk(
            godkjentÅrsakPermitteringFraFiskindustriId,
            "Årsaken til permitteringen fra fiskeindustrien er godkjent",
            synlig = erPermittertFraFisk(),
        )

    val erPermitteringenFraFiskeindustriMidlertidig =
        boolsk(
            erPermitteringenFraFiskeindustriMidlertidigId,
            "Permitteringen fra fiskeindustrien er midlertidig driftsinnskrenkning eller driftsstans",
            synlig = erPermittertFraFisk(),
        )

    val oppfyllerKravetTilPermitteringFiskeindustri =
        boolsk(
            oppfyllerKravetTilPermitteringFiskeindustriId,
            "Oppfyller kravet til permittering i fiskeindustrien",
        )

    val kravTilArbeidstidsreduksjonVedFiskepermittering =
        desimaltall(
            kravTilProsentvisTapAvArbeidstidFiskepermitteringId,
            "Krav til prosentvis tap av arbeidstid ved permittering fra fiskeindustrien",
        )

    val regelsett =
        vilkår(
            forskriftTilFolketrygden.hjemmel(
                kapittel = 6,
                paragraf = 7,
                tittel = "Permittering i fiskeforedlingsindustrien, sjømatindustrien og fiskeoljeindustrien",
                kortnavn = "Permittering fiskeindustri",
            ),
        ) {
            skalVurderes { it.erSann(kravTilAlder) && it.erSann(permitteringFiskeforedling) }

            regel(godkjentÅrsakPermitteringFraFiskindustri) { somUtgangspunkt(true) }
            regel(erPermitteringenFraFiskeindustriMidlertidig) { somUtgangspunkt(true) }
            regel(kravTilArbeidstidsreduksjonVedFiskepermittering) { oppslag { 40.0 } }

            utfall(oppfyllerKravetTilPermitteringFiskeindustri) {
                alle(
                    permitteringFiskeforedling,
                    godkjentÅrsakPermitteringFraFiskindustri,
                    erPermitteringenFraFiskeindustriMidlertidig,
                )
            }

            påvirkerResultat(erPermittertFraFisk())

            avklaring(HarOppgittPermitteringFiskeindustri)
        }

    private fun erPermittertFraFisk(): (LesbarOpplysninger) -> Boolean = { it.erSann(permitteringFiskeforedling) }

    val PermitteringFiskKontroll =
        Kontrollpunkt(HarOppgittPermitteringFiskeindustri) {
            it.har(permitteringFiskeforedling) && it.finnOpplysning(permitteringFiskeforedling).verdi
        }
}
