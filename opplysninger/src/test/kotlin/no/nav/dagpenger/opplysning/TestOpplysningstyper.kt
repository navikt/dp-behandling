package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.Opplysningstype.Id
import no.nav.dagpenger.opplysning.verdier.enhet.TimerEnhet
import no.nav.dagpenger.uuid.UUIDv7
import java.util.UUID

internal object TestOpplysningstyper {
    val dato1 by lazy { Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "dato1") }
    val dato2 by lazy { Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "dato2") }
    val desimaltall by lazy {
        Opplysningstype.desimaltall(
            Id(UUIDv7.ny(), Desimaltall),
            "desimaltall",
        )
    }
    val ulid by lazy { Opplysningstype.ulid(Id(UUIDv7.ny(), ULID), "ULID") }
    val a by lazy { Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "A") }
    val b by lazy { Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "B") }
    val c by lazy { Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "C") }
    val grunntall by lazy { Opplysningstype.beløp(Id(UUIDv7.ny(), Penger), "Grunntall") }
    val beløpA by lazy { Opplysningstype.beløp(Id(UUIDv7.ny(), Penger), "BeløpA") }
    val beløpB by lazy { Opplysningstype.beløp(Id(UUIDv7.ny(), Penger), "BeløpB") }
    val faktorA by lazy { Opplysningstype.desimaltall(Id(UUIDv7.ny(), Desimaltall), "FaktorA") }
    val faktorB by lazy { Opplysningstype.desimaltall(Id(UUIDv7.ny(), Desimaltall), "FaktorB") }
    val heltallA by lazy { Opplysningstype.heltall(Id(UUIDv7.ny(), Heltall), "HeltallA") }
    val produkt by lazy {
        Opplysningstype.desimaltall(
            Id(UUID.fromString("01948e45-6c8d-7c75-a07a-e8ded8560909"), Desimaltall),
            "Resultat",
        )
    }
    val andel by lazy { Opplysningstype.desimaltall(Id(UUIDv7.ny(), Desimaltall), "Andel") }
    val total by lazy { Opplysningstype.desimaltall(Id(UUIDv7.ny(), Desimaltall), "Total") }
    val terskel by lazy { Opplysningstype.desimaltall(Id(UUIDv7.ny(), Desimaltall), "Terskel") }

    val boolskB by lazy { Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "boolsk B") }
    val boolskC by lazy { Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "boolsk C") }
    val boolskA by lazy { Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "boolsk A") }
    val foreldrevilkår by lazy {
        Opplysningstype.boolsk(
            Id(UUIDv7.ny(), Boolsk),
            "Foreldrevilkår",
        )
    }
    val desimaltallSomTimer by lazy {
        Opplysningstype.desimaltall(
            Id(UUIDv7.ny(), Desimaltall),
            "desimaltallSomTimer",
            enhet = TimerEnhet,
        )
    }
    val undervilkår1 by lazy { Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "Undervilkår1") }
    val undervilkår2 by lazy { Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "Undervilkår2") }
}
