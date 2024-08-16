package no.nav.dagpenger.opplysning.verdier

import org.javamoney.moneta.Money
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.money.Monetary
import javax.money.MonetaryContextBuilder
import javax.money.NumberValue
import javax.money.RoundingQueryBuilder
import javax.money.convert.MonetaryConversions

class Beløp private constructor(
    private val verdi: Money,
) : Comparable<Beløp> {
    constructor(verdi: String) : this(Money.parse(verdi))
    constructor(verdi: Double) : this(BigDecimal.valueOf(verdi))
    constructor(verdi: BigDecimal) : this(
        Money.of(
            verdi,
            Monetary.getCurrency("NOK"),
            MonetaryContextBuilder
                .of()
                .set(MathContext.UNLIMITED)
                .setMaxScale(DECIMAL_PRESISJON)
                .set(RoundingMode.HALF_UP)
                .build(),
        ),
    )

    val uavrundet: NumberValue get() = verdi.number

    val avrundet: NumberValue get() = verdi.with(ører).number
    val heleKroner: NumberValue get() = verdi.with(kroner).number

    val verdien: BigDecimal get() = verdi.number.numberValueExact(BigDecimal::class.java)

    operator fun plus(other: Beløp): Beløp = Beløp(verdi.add(other.verdi))

    operator fun div(faktor: Double): Beløp = Beløp(verdi.divide(faktor))

    operator fun div(faktor: Beløp): Beløp = Beløp(verdi.divide(faktor.verdien))

    operator fun times(faktor: Double): Beløp = Beløp(verdi.multiply(faktor))

    operator fun times(faktor: Int): Beløp = Beløp(verdi.multiply(faktor))

    override fun compareTo(other: Beløp): Int = verdi.compareTo(other.verdi)

    override fun equals(other: Any?): Boolean = other is Beløp && verdi == other.verdi

    override fun hashCode(): Int = verdi.hashCode()

    override fun toString(): String = verdi.toString()

    fun somKroner(): Beløp {
        val rateProvider = MonetaryConversions.getExchangeRateProvider()
        val vekslingkurs = rateProvider.getCurrencyConversion("NOK")

        return Beløp(verdi.with(vekslingkurs))
    }

    private companion object {
        private const val DECIMAL_PRESISJON = 20
        private val ører =
            Monetary.getRounding(
                RoundingQueryBuilder
                    .of()
                    .setScale(0)
                    .set(RoundingMode.HALF_UP)
                    .build(),
            )
        private val kroner =
            Monetary.getRounding(
                RoundingQueryBuilder
                    .of()
                    .setScale(0)
                    .set(RoundingMode.HALF_UP)
                    .build(),
            )
    }
}
