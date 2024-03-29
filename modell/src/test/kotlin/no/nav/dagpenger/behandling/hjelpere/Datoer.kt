package no.nav.dagpenger.behandling.hjelpere

import java.time.LocalDate
import java.time.YearMonth

internal fun januar(år: Int) = YearMonth.of(år, 1)

internal infix fun Int.januar(år: Int) = LocalDate.of(år, 1, this)

internal val Int.januar get() = this.januar(2022)

internal fun februar(år: Int) = YearMonth.of(år, 2)

internal infix fun Int.februar(år: Int) = LocalDate.of(år, 2, this)

internal val Int.februar get() = this.februar(2022)

internal fun mars(år: Int) = YearMonth.of(år, 3)

internal infix fun Int.mars(år: Int) = LocalDate.of(år, 3, this)

internal val Int.mars get() = this.mars(2022)

internal fun april(år: Int) = YearMonth.of(år, 4)

internal infix fun Int.april(år: Int) = LocalDate.of(år, 4, this)

internal val Int.april get() = this.april(2022)

internal fun mai(år: Int) = YearMonth.of(år, 5)

internal infix fun Int.mai(år: Int) = LocalDate.of(år, 5, this)

internal val Int.mai get() = this.mai(2022)

internal fun juni(år: Int) = YearMonth.of(år, 6)

internal infix fun Int.juni(år: Int) = LocalDate.of(år, 6, this)

internal val Int.juni get() = this.juni(2022)

internal fun juli(år: Int) = YearMonth.of(år, 7)

internal infix fun Int.juli(år: Int) = LocalDate.of(år, 7, this)

internal val Int.juli get() = this.juli(2022)

internal fun august(år: Int) = YearMonth.of(år, 8)

internal infix fun Int.august(år: Int) = LocalDate.of(år, 8, this)

internal val Int.august get() = this.august(2022)

internal fun september(år: Int) = YearMonth.of(år, 9)

internal infix fun Int.september(år: Int) = LocalDate.of(år, 9, this)

internal val Int.september get() = this.september(2022)

internal fun oktober(år: Int) = YearMonth.of(år, 10)

internal infix fun Int.oktober(år: Int) = LocalDate.of(år, 10, this)

internal val Int.oktober get() = this.oktober(2022)

internal fun november(år: Int) = YearMonth.of(år, 11)

internal infix fun Int.november(år: Int) = LocalDate.of(år, 11, this)

internal val Int.november get() = this.november(2022)

internal fun desember(år: Int) = YearMonth.of(år, 12)

internal infix fun Int.desember(år: Int) = LocalDate.of(år, 12, this)

internal val Int.desember get() = this.desember(2022)
