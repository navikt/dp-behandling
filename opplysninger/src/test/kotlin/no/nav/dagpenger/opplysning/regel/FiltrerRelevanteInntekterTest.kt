package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.collections.shouldHaveSize
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.inntekt.FiltrerRelevanteInntekter
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class FiltrerRelevanteInntekterTest {
    private val inntektA = Opplysningstype.inntekt(Opplysningstype.Id(UUIDv7.ny(), InntektDataType), "inntektA")
    private val inntektB = Opplysningstype.inntekt(Opplysningstype.Id(UUIDv7.ny(), InntektDataType), "inntektB")

    @Test
    fun `skal filtrere ut innteksklasser`() {
        val opplysninger = Opplysninger()
        val regel =
            FiltrerRelevanteInntekter(
                produserer = inntektB,
                ufiltrertInntekt = inntektA,
                inntektsklasser =
                    listOf(
                        no.nav.dagpenger.inntekt.v1.InntektKlasse.ARBEIDSINNTEKT,
                    ),
            )

        opplysninger.leggTil(
            Faktum(
                inntektA,
                Inntekt(
                    verdi,
                ),
            ),
        )

        val filtrertInntekt = regel.lagProdukt(opplysninger, LocalDate.now())
        filtrertInntekt
            .verdi.verdi.inntektsListe
            .flatMap { it.klassifiserteInntekter } shouldHaveSize 1
    }

    private val verdi =
        no.nav.dagpenger.inntekt.v1.Inntekt(
            inntektsId = "01J677GHJRC2H08Q55DASFD0XX",
            inntektsListe =
                listOf(
                    KlassifisertInntektMåned(
                        årMåned = YearMonth.from(LocalDate.now()),
                        klassifiserteInntekter =
                            listOf(
                                no.nav.dagpenger.inntekt.v1.KlassifisertInntekt(
                                    beløp = 10000.toBigDecimal(),
                                    inntektKlasse = no.nav.dagpenger.inntekt.v1.InntektKlasse.ARBEIDSINNTEKT,
                                ),
                                no.nav.dagpenger.inntekt.v1.KlassifisertInntekt(
                                    beløp = 10000.toBigDecimal(),
                                    inntektKlasse = no.nav.dagpenger.inntekt.v1.InntektKlasse.SYKEPENGER,
                                ),
                            ),
                        harAvvik = false,
                    ),
                    KlassifisertInntektMåned(
                        årMåned = YearMonth.from(LocalDate.now()).minusMonths(1),
                        klassifiserteInntekter =
                            listOf(
                                no.nav.dagpenger.inntekt.v1.KlassifisertInntekt(
                                    beløp = 10000.toBigDecimal(),
                                    inntektKlasse = no.nav.dagpenger.inntekt.v1.InntektKlasse.SYKEPENGER,
                                ),
                            ),
                        harAvvik = false,
                    ),
                ),
            sisteAvsluttendeKalenderMåned = YearMonth.from(LocalDate.now()),
        )
}
