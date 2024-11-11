package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
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
import no.nav.dagpenger.opplysning.tekstId
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Behov.InntektId
import no.nav.dagpenger.regel.Behov.OpptjeningsperiodeFraOgMed
import no.nav.dagpenger.regel.GrenseverdierForMinsteArbeidsinntekt.finnTerskel
import no.nav.dagpenger.regel.Opptjeningstid.justertRapporteringsfrist
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadstidspunkt
import java.time.LocalDate

object Minsteinntekt {
    private val `12mndTerskelFaktor` =
        Opplysningstype.somDesimaltall(
            "Antall G for krav til 12 mnd arbeidsinntekt".tekstId("opplysning.antall-g-for-krav-til-12-mnd-arbeidsinntekt"),
        )
    private val `36mndTerskelFaktor` =
        Opplysningstype.somDesimaltall(
            "Antall G for krav til 36 mnd arbeidsinntekt".tekstId("opplysning.antall-g-for-krav-til-36-mnd-arbeidsinntekt"),
        )
    val inntekt12 =
        Opplysningstype.somBeløp(
            "Arbeidsinntekt siste 12 mnd".id(
                id = "InntektSiste12Mnd",
                tekstId = "opplysning.arbeidsinntekt-siste-12-mnd",
            ),
        )
    val inntekt36 =
        Opplysningstype.somBeløp(
            "Arbeidsinntekt siste 36 mnd".id(
                id = "InntektSiste36Mnd",
                tekstId = "opplysning.arbeidsinntekt-siste-36-mnd",
            ),
        )
    val grunnbeløp = Opplysningstype.somBeløp("Grunnbeløp")

    private val sisteAvsluttendendeKalenderMåned = Opptjeningstid.sisteAvsluttendendeKalenderMåned
    internal val inntektId = Opplysningstype.somUlid("Inntekt".id(InntektId))
    private val maksPeriodeLengde = Opplysningstype.somHeltall("Maks lengde på opptjeningsperiode")
    private val førsteMånedAvOpptjeningsperiode =
        Opplysningstype.somDato("Første måned av opptjeningsperiode".id(OpptjeningsperiodeFraOgMed))

    private val `12mndTerskel` =
        Opplysningstype.somBeløp(
            "Inntektskrav for siste 12 mnd".tekstId("opplysning.inntektskrav-for-siste-12-mnd"),
        )
    private val `36mndTerskel` =
        Opplysningstype.somBeløp(
            "Inntektskrav for siste 36 mnd".tekstId("opplysning.inntektskrav-for-siste-36-mnd"),
        )
    private val over12mndTerskel = Opplysningstype.somBoolsk("Arbeidsinntekt er over kravet for siste 12 mnd")
    private val over36mndTerskel = Opplysningstype.somBoolsk("Arbeidsinntekt er over kravet for siste 36 mnd")

    private val verneplikt = Verneplikt.vurderingAvVerneplikt
    val minsteinntekt = Opplysningstype.somBoolsk("Krav til minsteinntekt".tekstId("opplysning.krav-til-minsteinntekt"))

    val regelsett =
        Regelsett("Minsteinntekt") {
            regel(maksPeriodeLengde) { oppslag(prøvingsdato) { 36 } }
            regel(førsteMånedAvOpptjeningsperiode) { trekkFraMånedTilFørste(sisteAvsluttendendeKalenderMåned, maksPeriodeLengde) }
            regel(inntektId) { innhentMed(prøvingsdato, sisteAvsluttendendeKalenderMåned, førsteMånedAvOpptjeningsperiode) }

            regel(grunnbeløp) { oppslag(prøvingsdato) { grunnbeløpFor(it) } }

            regel(inntekt12) { innhentMed(inntektId) }
            regel(`12mndTerskelFaktor`) { oppslag(prøvingsdato) { finnTerskel(it).nedre } }
            regel(`12mndTerskel`) { multiplikasjon(grunnbeløp, `12mndTerskelFaktor`) }
            regel(over12mndTerskel) { størreEnnEllerLik(inntekt12, `12mndTerskel`) }

            regel(inntekt36) { innhentMed(inntektId) }
            regel(`36mndTerskelFaktor`) { oppslag(prøvingsdato) { finnTerskel(it).øvre } }
            regel(`36mndTerskel`) { multiplikasjon(grunnbeløp, `36mndTerskelFaktor`) }
            regel(over36mndTerskel) { størreEnnEllerLik(inntekt36, `36mndTerskel`) }

            regel(minsteinntekt) { enAv(over12mndTerskel, over36mndTerskel, verneplikt) }
        }

    private fun grunnbeløpFor(it: LocalDate) =
        getGrunnbeløpForRegel(Regel.Minsteinntekt)
            .forDato(it)
            .verdi
            .let { Beløp(it) }

    val SvangerskapsrelaterteSykepengerKontroll =
        Kontrollpunkt(Avklaringspunkter.SvangerskapsrelaterteSykepenger) { it.har(inntektId) }

    val EØSArbeidKontroll =
        Kontrollpunkt(Avklaringspunkter.EØSArbeid) { it.har(inntektId) }

    val JobbetUtenforNorgeKontroll =
        Kontrollpunkt(Avklaringspunkter.JobbetUtenforNorge) { it.har(inntektId) }

    val InntektNesteKalendermånedKontroll =
        Kontrollpunkt(Avklaringspunkter.InntektNesteKalendermåned) { it.har(inntektId) }

    val ØnskerEtterRapporteringsfristKontroll =
        Kontrollpunkt(Avklaringspunkter.ØnskerEtterRapporteringsfrist) {
            if (!it.har(justertRapporteringsfrist)) return@Kontrollpunkt false
            if (!it.har(søknadstidspunkt)) return@Kontrollpunkt false
            if (!it.har(søknadsdato)) return@Kontrollpunkt false

            val rapporteringsfrist = it.finnOpplysning(justertRapporteringsfrist).verdi
            val søknadstidspunkt = it.finnOpplysning(søknadstidspunkt).verdi
            val søknadsdato = it.finnOpplysning(søknadsdato).verdi

            søknadstidspunkt > rapporteringsfrist && søknadsdato <= rapporteringsfrist
        }
}
