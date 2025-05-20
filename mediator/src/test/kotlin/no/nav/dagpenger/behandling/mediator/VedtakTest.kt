package no.nav.dagpenger.behandling.mediator

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainOnly
import no.nav.dagpenger.behandling.api.models.VilkaarNavnDTO
import no.nav.dagpenger.regel.RegelverkDagpenger
import org.junit.jupiter.api.Test

class VedtakTest {
    @Test
    fun `har mapping for alle vilkår`() {
        withClue(
            "Mangler mapping fra vilkår til opplysning ${VilkaarNavnDTO.entries.toSet() - opplysningTilVilkårMap.keys}",
        ) {
            opplysningTilVilkårMap.values.toSet() shouldContainOnly VilkaarNavnDTO.entries.toSet()
        }

        val vilkårsopplysninger = RegelverkDagpenger.vilkårsopplysninger
        withClue(
            "Mangler mapping fra opplysning til vilkår ${vilkårsopplysninger.toSet() - opplysningTilVilkårMap.keys}",
        ) {
            opplysningTilVilkårMap.keys.toSet() shouldContainOnly vilkårsopplysninger.toSet()
        }
    }
}
