package no.nav.dagpenger.dato

import no.bekk.bekkopen.date.NorwegianDateUtil
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

tailrec fun finnFørsteArbeidsdag(dato: LocalDate): LocalDate =
    if (dato.arbeidsdag()) {
        dato
    } else {
        finnFørsteArbeidsdag(dato.plusDays(1))
    }

private fun LocalDate.arbeidsdag(): Boolean =
    NorwegianDateUtil.isWorkingDay(Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant()))
