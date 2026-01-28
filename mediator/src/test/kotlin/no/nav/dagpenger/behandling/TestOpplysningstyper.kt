package no.nav.dagpenger.behandling

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.dagpenger.behandling.mediator.repository.OpplysningerRepositoryPostgres
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.definerteTyper
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.uuid.UUIDv7
import java.util.UUID
import javax.sql.DataSource
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertTrue

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

    fun opplysningerRepository(dataSource: DataSource): OpplysningerRepositoryPostgres =
        OpplysningerRepositoryPostgres(dataSource).apply {
            lagreOpplysningstyper(definerteTyper)
        }

    @Test
    fun `skal finne alle id-er i OpplysningsTyper`() {
        val opplysningsTyperClass = OpplysningsTyper::class
        val opplysningstyper = opplysningsTyperClass.memberProperties.filter { it.returnType.toString().contains("Opplysningstype.Id") }

        val uuidSet = mutableSetOf<UUID>()

        opplysningstyper.forEach { opplysning ->
            val idVerdi = opplysning.get(OpplysningsTyper) as Opplysningstype.Id<*>
            idVerdi shouldNotBeNull { "Opplysningstype ${opplysning.name} kan ikke være null" }
            assertTrue(uuidSet.add(idVerdi.uuid), "Opplysningstype ${opplysning.name} sin uuid er ikke unik")
        }
    }
}
