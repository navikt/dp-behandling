package no.nav.dagpenger.regel

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.dagpenger.opplysning.Opplysningstype
import java.util.UUID
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertTrue

class OpplysningsTyperTest {
    @Test
    fun `skal finne alle id-er i OpplysningsTyper og verifisere at de er unike`() {
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
