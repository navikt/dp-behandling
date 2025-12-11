package no.nav.dagpenger.opplysning.regel.inntekt

import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.verdier.Inntekt

// TODO : Flytt denne til dagpenger-regelverks modul
class FiltrerRelevanteInntekter(
    produserer: Opplysningstype<Inntekt>,
    private val ufiltrertInntekt: Opplysningstype<Inntekt>,
    private val inntektsklasser: List<InntektKlasse>,
) : Regel<Inntekt>(produserer, listOf(ufiltrertInntekt)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Inntekt {
        val inntekt = opplysninger.finnOpplysning(this.ufiltrertInntekt).verdi
        val relevanteInntekter =
            inntekt.verdi.inntektsListe.map {
                KlassifisertInntektMåned(
                    årMåned = it.årMåned,
                    klassifiserteInntekter =
                        it.klassifiserteInntekter.filter { inntektMåned ->
                            inntektMåned.inntektKlasse in inntektsklasser
                        },
                    harAvvik = it.harAvvik,
                )
            }
        return Inntekt(
            no.nav.dagpenger.inntekt.v1.Inntekt(
                inntektsId = inntekt.verdi.inntektsId,
                inntektsListe = relevanteInntekter,
                sisteAvsluttendeKalenderMåned = inntekt.verdi.sisteAvsluttendeKalenderMåned,
                hentetTidspunkt = inntekt.verdi.hentetTidspunkt,
            ),
        )
    }

    override fun toString() = "Filtrerer inntekter for $ufiltrertInntekt med inntektsklasser ${inntektsklasser.joinToString { it.name }}"
}

fun Opplysningstype<Inntekt>.filtrerRelevanteInntekter(
    ufiltrertInntekt: Opplysningstype<Inntekt>,
    inntektklasser: List<InntektKlasse>,
) = FiltrerRelevanteInntekter(
    this,
    ufiltrertInntekt,
    inntektklasser,
)
