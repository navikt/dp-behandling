package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.verdier.Ulid
import java.time.LocalDate

sealed class Datatype<T : Comparable<T>>(val klasse: Class<T>)

data object Dato : Datatype<LocalDate>(LocalDate::class.java)

data object Desimaltall : Datatype<Double>(Double::class.java)

data object Heltall : Datatype<Int>(Int::class.java)

data object Boolsk : Datatype<Boolean>(Boolean::class.java)

data object Ulid : Datatype<Ulid>(Ulid::class.java)
