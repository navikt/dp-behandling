package no.nav.dagpenger.mediator

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import no.nav.dagpenger.opplysning.Opplysningstype
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Verifiserer at alle statisk-definerte [Opplysningstype.Id]-vals på tvers av moduler
 * har unike UUID-er. Dette erstatter den tidligere runtime-sjekken i
 * `Opplysningstype.init`, som krevde en global mutable `definerteTyper`.
 *
 * Sjekken kjører compile/test-time mot kjente `OpplysningsTyper`-objekter funnet
 * via classpath-scanning og er trygg å kjøre parallelt.
 */
class OpplysningstypeIdUnikhetTest {
    private val opplysningsTyperObjekter: List<KClass<*>> =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("no.nav.dagpenger")
            .filter { it.simpleName == "OpplysningsTyper" }
            .map { Class.forName(it.fullName).kotlin }

    @Test
    fun `finner OpplysningsTyper-objekter i klassebanen`() {
        opplysningsTyperObjekter.shouldNotBeEmpty()
    }

    @Test
    fun `alle Opplysningstype Id-er har unike UUID-er på tvers av moduler`() {
        data class IdReferanse(
            val kilde: String,
            val navn: String,
            val uuid: UUID,
        )

        val alleIder: List<IdReferanse> =
            opplysningsTyperObjekter.flatMap { kclass ->
                val instans = kclass.objectInstance ?: return@flatMap emptyList()
                kclass.memberProperties
                    .mapNotNull { prop ->
                        val verdi = prop.getter.call(instans) as? Opplysningstype.Id<*> ?: return@mapNotNull null
                        IdReferanse(kilde = kclass.qualifiedName ?: kclass.simpleName.orEmpty(), navn = prop.name, uuid = verdi.uuid)
                    }
            }

        val duplikater =
            alleIder
                .groupBy { it.uuid }
                .filterValues { it.size > 1 }
                .map { (uuid, referanser) ->
                    val visning = referanser.joinToString(", ") { "${it.kilde}.${it.navn}" }
                    "UUID $uuid brukes av flere opplysninger: $visning"
                }

        duplikater.shouldBeEmpty()
    }
}
