package no.nav.dagpenger.opplysning.regel.inntekt

import no.nav.dagpenger.grunnbelop.forMåned
import no.nav.dagpenger.grunnbelop.getGrunnbeløpForRegel
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import java.time.LocalDate

// TODO : Flytt denne til dagpenger-regelverks modul
class Oppjuster(
    produserer: Opplysningstype<Inntekt>,
    private val grunnbeløp: Opplysningstype<Beløp>,
    private val inntekt: Opplysningstype<Inntekt>,
) : Regel<Inntekt>(produserer, listOf(grunnbeløp, inntekt)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Inntekt {
        val søknadstidspunktetsGrunnbeløp = opplysninger.finnOpplysning(this.grunnbeløp).verdi
        val inntekt = opplysninger.finnOpplysning(this.inntekt).verdi

        val oppjusterteinntekter: List<KlassifisertInntektMåned> =
            inntekt.verdi.inntektsListe.map { klassifisertInntektMåned ->
                val måned = klassifisertInntektMåned.årMåned
                val grunnbeløpForMåned =
                    Beløp(getGrunnbeløpForRegel(no.nav.dagpenger.grunnbelop.Regel.Grunnlag).forMåned(måned).verdi)

                val oppjustert =
                    klassifisertInntektMåned.klassifiserteInntekter.map { postering ->
                        val faktor = søknadstidspunktetsGrunnbeløp / grunnbeløpForMåned
                        val oppjustert = postering.beløp.multiply(faktor.verdien)
                        postering.copy(oppjustert)
                    }
                klassifisertInntektMåned.copy(klassifiserteInntekter = oppjustert)
            }

        val oppjustertInntekt =
            no.nav.dagpenger.inntekt.v1.Inntekt(
                inntektsId = inntekt.verdi.inntektsId,
                inntektsListe = oppjusterteinntekter,
                sisteAvsluttendeKalenderMåned = inntekt.verdi.sisteAvsluttendeKalenderMåned,
            )
        return Inntekt(oppjustertInntekt)
    }

    override fun toString() = "Oppjusterer inntekt for $inntekt med grunnbeløp $grunnbeløp"
}

fun Opplysningstype<Inntekt>.oppjuster(
    grunnbeløp: Opplysningstype<Beløp>,
    inntekt: Opplysningstype<Inntekt>,
) = Oppjuster(this, grunnbeløp, inntekt)
