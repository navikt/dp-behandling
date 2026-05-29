package no.nav.dagpenger.mediator

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.OpplysningstypeRegister
import no.nav.dagpenger.regelverk.RegelverkRegistrering
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.lang.reflect.Modifier
import java.util.UUID

/**
 * Verifiserer at alle statisk-deklarerte [Opplysningstype]- og [Opplysningstype.Id]-vals
 * i hovedkoden har unike UUID-er. Erstatter den tidligere runtime-sjekken i
 * `Opplysningstype.init` (som krevde global mutable `definerteTyper`) og er trygg
 * å kjøre parallelt.
 *
 * Skanner *alle* klasser i `no.nav.dagpenger`, ikke bare `OpplysningsTyper`-objekter,
 * slik at top-level vals i vilkårlige filer også fanges opp.
 */
class OpplysningstypeIdUnikhetTest {
    private data class Felt(
        val kilde: String,
        val uuid: UUID,
        val datatype: String,
        val navn: String?,
        val instans: Any,
    )

    private val alleStatiskeOpplysningstypeFelter: List<Felt> by lazy {
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("no.nav.dagpenger")
            .map { it.fullName }
            .distinct()
            .flatMap { fqn -> feltVerdierI(fqn) }
    }

    private fun feltVerdierI(fqn: String): List<Felt> {
        val klasse =
            runCatching { Class.forName(fqn) }.getOrNull() ?: return emptyList()
        val instans = runCatching { klasse.kotlin.objectInstance }.getOrNull()
        return klasse.declaredFields.mapNotNull { field ->
            if (!Modifier.isStatic(field.modifiers) && instans == null) return@mapNotNull null
            field.isAccessible = true
            val verdi =
                runCatching {
                    if (Modifier.isStatic(field.modifiers)) field.get(null) else field.get(instans)
                }.getOrNull() ?: return@mapNotNull null
            val kilde = "${klasse.name}.${field.name}"
            when (verdi) {
                is Opplysningstype<*> -> Felt(kilde, verdi.id.uuid, verdi.datatype.toString(), verdi.navn, verdi.id)
                is Opplysningstype.Id<*> -> Felt(kilde, verdi.uuid, verdi.datatype.toString(), null, verdi)
                else -> null
            }
        }
    }

    @Test
    fun `finner opplysningstype-felter i klassebanen`() {
        alleStatiskeOpplysningstypeFelter.shouldNotBeEmpty()
    }

    @Test
    fun `alle statiske opplysningstype-vals har unike UUID-er`() {
        // Grupper per UUID, men telle bare hver instans én gang (samme instans brukt fra flere
        // felter er gjenbruk, ikke duplisering). Et duplikat er to *forskjellige* instanser
        // (Id eller Opplysningstype) som deler UUID.
        val duplikater =
            alleStatiskeOpplysningstypeFelter
                .groupBy { it.uuid }
                .mapValues { (_, felter) -> felter.distinctBy { System.identityHashCode(it.instans) } }
                .filterValues { it.size > 1 }
                .map { (uuid, felter) ->
                    val visning = felter.joinToString(", ") { "${it.kilde} (${it.datatype}, navn=${it.navn})" }
                    "UUID $uuid deles av: $visning"
                }
        duplikater.shouldBeEmpty()
    }

    @Test
    fun `register bygget fra alle regelverk har ingen UUID-konflikter`() {
        val alleTyper =
            ClassFileImporter()
                .withImportOption(ImportOption.DoNotIncludeTests())
                .importPackages("no.nav.dagpenger")
                .filter { javaClass ->
                    runCatching {
                        val k = Class.forName(javaClass.fullName)
                        !Modifier.isAbstract(k.modifiers) &&
                            RegelverkRegistrering::class.java.isAssignableFrom(k) &&
                            k != RegelverkRegistrering::class.java
                    }.getOrDefault(false)
                }.mapNotNull { javaClass ->
                    runCatching {
                        Class
                            .forName(
                                javaClass.fullName,
                            ).getDeclaredConstructor()
                            .apply { isAccessible = true }
                            .newInstance() as RegelverkRegistrering
                    }.getOrNull()
                }.flatMap { it.opplysningstyper }

        // OpplysningstypeRegister.av(...) kaster ved UUID-konflikt
        assertDoesNotThrow {
            OpplysningstypeRegister.av(alleTyper)
        }
    }
}
