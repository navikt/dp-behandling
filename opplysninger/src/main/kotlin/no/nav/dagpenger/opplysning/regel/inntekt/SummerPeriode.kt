package no.nav.dagpenger.opplysning.regel.inntekt

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt

class SummerPeriode(
    produserer: Opplysningstype<Beløp>,
    private val inntekt: Opplysningstype<Inntekt>,
    private val periode: Set<InntektPeriode>,
) : Regel<Beløp>(produserer, listOf(inntekt)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Beløp {
        val inntekt = opplysninger.finnOpplysning(inntekt).verdi
        return periode.map { it.block(inntekt.verdi) }.reduce { acc, beløp -> acc + beløp }
    }

    override fun toString() = "Summerer periode ${periode.joinToString { "$it" }} av inntekt $inntekt"

    enum class InntektPeriode(
        val block: (no.nav.dagpenger.inntekt.v1.Inntekt) -> Beløp,
    ) {
        Første({ inntekt ->
            Beløp(
                inntekt.splitIntoInntektsPerioder().first.sumOf { klassifisertInntektMåned ->
                    klassifisertInntektMåned.klassifiserteInntekter.sumOf { it.beløp }
                },
            )
        }),
        Andre({ inntekt ->
            Beløp(
                inntekt.splitIntoInntektsPerioder().second.sumOf { klassifisertInntektMåned ->
                    klassifisertInntektMåned.klassifiserteInntekter.sumOf { it.beløp }
                },
            )
        }),
        Tredje({ inntekt ->
            Beløp(
                inntekt.splitIntoInntektsPerioder().third.sumOf { klassifisertInntektMåned ->
                    klassifisertInntektMåned.klassifiserteInntekter.sumOf { it.beløp }
                },
            )
        }),
    }
}

fun Opplysningstype<Beløp>.summerPeriode(
    inntekt: Opplysningstype<Inntekt>,
    vararg periode: SummerPeriode.InntektPeriode,
) = SummerPeriode(
    this,
    inntekt,
    periode.toSet(),
)
