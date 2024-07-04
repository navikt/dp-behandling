package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.behandling.konklusjon.KonklusjonsSjekk.Resultat.IkkeKonkludert
import no.nav.dagpenger.behandling.konklusjon.KonklusjonsSjekk.Resultat.Konkludert
import no.nav.dagpenger.behandling.konklusjon.KonklusjonsStrategi
import no.nav.dagpenger.grunnbelop.Regel
import no.nav.dagpenger.grunnbelop.forDato
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.dato.trekkFraMånedTilFørste
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.regel.Behov.InntektId
import no.nav.dagpenger.regel.Behov.OpptjeningsperiodeFraOgMed
import no.nav.dagpenger.regel.GrenseverdierForMinsteArbeidsinntekt.finnTerskel
import java.time.LocalDate

object Minsteinntekt {
    private val `12mndTerskelFaktor` = Opplysningstype.somDesimaltall("Antall G for krav til 12 mnd arbeidsinntekt")
    private val `36mndTerskelFaktor` = Opplysningstype.somDesimaltall("Antall G for krav til 36 mnd arbeidsinntekt")
    val inntekt12 = Opplysningstype.somDesimaltall("Arbeidsinntekt siste 12 mnd".id("InntektSiste12Mnd"))
    val inntekt36 = Opplysningstype.somDesimaltall("Arbeidsinntekt siste 36 mnd".id("InntektSiste36Mnd"))
    private val grunnbeløp = Opplysningstype.somDesimaltall("Grunnbeløp")

    private val sisteAvsluttendendeKalenderMåned = Opptjeningstid.sisteAvsluttendendeKalenderMåned
    private val inntektId = Opplysningstype.somUlid("Inntekt".id(InntektId))
    private val maksPeriodeLengde = Opplysningstype.somHeltall("Maks lengde på opptjeningsperiode")
    private val førsteMånedAvOpptjeningsperiode =
        Opplysningstype.somDato("Første måned av opptjeningsperiode".id(OpptjeningsperiodeFraOgMed))

    private val virkningsdato = Søknadstidspunkt.søknadstidspunkt
    private val `12mndTerskel` = Opplysningstype.somDesimaltall("Inntektskrav for siste 12 mnd")
    private val `36mndTerskel` = Opplysningstype.somDesimaltall("Inntektskrav for siste 36 mnd")
    private val over12mndTerskel = Opplysningstype.somBoolsk("Arbeidsinntekt er over kravet for siste 12 mnd")
    private val over36mndTerskel = Opplysningstype.somBoolsk("Arbeidsinntekt er over kravet for siste 36 mnd")

    private val verneplikt = Verneplikt.vurderingAvVerneplikt
    val minsteinntekt = Opplysningstype.somBoolsk("Krav til minsteinntekt")

    val regelsett =
        Regelsett("Minsteinntekt") {
            regel(maksPeriodeLengde) { oppslag(virkningsdato) { 36 } }
            regel(førsteMånedAvOpptjeningsperiode) { trekkFraMånedTilFørste(sisteAvsluttendendeKalenderMåned, maksPeriodeLengde) }
            regel(inntektId) { innhentMed(virkningsdato, sisteAvsluttendendeKalenderMåned, førsteMånedAvOpptjeningsperiode) }

            regel(grunnbeløp) { oppslag(virkningsdato) { grunnbeløpFor(it) } }

            regel(inntekt12) { innhentMed(inntektId) }
            regel(`12mndTerskelFaktor`) { oppslag(virkningsdato) { finnTerskel(it).nedre } }
            regel(`12mndTerskel`) { multiplikasjon(`12mndTerskelFaktor`, grunnbeløp) }
            regel(over12mndTerskel) { størreEnnEllerLik(inntekt12, `12mndTerskel`) }

            regel(inntekt36) { innhentMed(inntektId) }
            regel(`36mndTerskelFaktor`) { oppslag(virkningsdato) { finnTerskel(it).øvre } }
            regel(`36mndTerskel`) { multiplikasjon(`36mndTerskelFaktor`, grunnbeløp) }
            regel(over36mndTerskel) { størreEnnEllerLik(inntekt36, `36mndTerskel`) }

            regel(minsteinntekt) { enAv(over12mndTerskel, over36mndTerskel, verneplikt) }
        }

    private fun grunnbeløpFor(it: LocalDate) =
        getGrunnbeløpForRegel(Regel.Minsteinntekt)
            .forDato(it)
            .verdi
            // TODO: Bli enige med oss selv hva som er Double og BigDecimal
            .toDouble()

    val SvangerskapsrelaterteSykepengerKontroll =
        Kontrollpunkt(Avklaringspunkter.SvangerskapsrelaterteSykepenger) { it.har(inntektId) }

    val EØSArbeidKontroll =
        Kontrollpunkt(Avklaringspunkter.EØSArbeid) { it.har(inntektId) }

    val JobbetUtenforNorgeKontroll =
        Kontrollpunkt(Avklaringspunkter.JobbetUtenforNorge) { it.har(inntektId) }

    val InntektNesteKalendermånedKontroll =
        Kontrollpunkt(Avklaringspunkter.InntektNesteKalendermåned) { it.har(inntektId) }

    val AvslagInntekt =
        KonklusjonsStrategi(DagpengerKonklusjoner.AvslagMinsteinntekt) { opplysninger ->
            if (opplysninger.mangler(minsteinntekt)) return@KonklusjonsStrategi IkkeKonkludert
            if (!opplysninger.finnOpplysning(minsteinntekt).verdi) {
                return@KonklusjonsStrategi Konkludert
            } else {
                IkkeKonkludert
            }
        }
}
