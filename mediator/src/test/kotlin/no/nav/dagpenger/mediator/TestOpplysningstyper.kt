package no.nav.dagpenger.mediator

import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.mediator.repository.KildeRepository
import no.nav.dagpenger.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.OpplysningstypeRegister
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.uuid.UUIDv7

internal object TestOpplysningstyper {
    val baseOpplysningstype = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "Base")
    val utledetOpplysningstype = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Utledet")
    val maksdato = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "MaksDato")
    val mindato = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "MinDato")
    val heltall = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "heltall")
    val boolsk = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "boolsk")
    val dato = Opplysningstype.dato(Opplysningstype.Id(UUIDv7.ny(), Dato), "Dato")
    val desimal =
        Opplysningstype.desimaltall(
            Opplysningstype.Id(UUIDv7.ny(), Desimaltall),
            beskrivelse = "Desimal",
            behovId = "desimaltall",
        )
    val inntektA = Opplysningstype.inntekt(Opplysningstype.Id(UUIDv7.ny(), InntektDataType), "Inntekt")
    val tekst = Opplysningstype.tekst(Opplysningstype.Id(UUIDv7.ny(), Tekst), "Tekst")
    val barn =
        Opplysningstype.barn(
            Opplysningstype.Id(UUIDv7.ny(), BarnDatatype),
            "Barn",
            behovId = "BarnetilleggV2",
            utgåtteBehovId = setOf("Barnetillegg"),
        )
    val periode = Opplysningstype.periode(Opplysningstype.Id(UUIDv7.ny(), PeriodeDataType), "Periode")

    val beløpA = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "BeløpA")
    val beløpB = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "BeløpB")

    val alle: Set<Opplysningstype<*>> =
        setOf(
            baseOpplysningstype,
            utledetOpplysningstype,
            maksdato,
            mindato,
            heltall,
            boolsk,
            dato,
            desimal,
            inntektA,
            tekst,
            barn,
            periode,
            beløpA,
            beløpB,
        )

    val register: OpplysningstypeRegister = OpplysningstypeRegister.av(alle)

    fun opplysningerRepository(
        dataSource: DatabaseSession,
        ekstraTyper: Collection<Opplysningstype<*>> = emptyList(),
    ): OpplysningerRepositoryPostgres {
        val testregister = OpplysningstypeRegister.av(register.alle + ekstraTyper)
        return OpplysningerRepositoryPostgres(dataSource, KildeRepository(dataSource), testregister).apply {
            lagreOpplysningstyper(testregister.alle.toList())
        }
    }
}
