package no.nav.dagpenger.modell.hendelser

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.hendelser.OpplysningSvar.Tilstand.Faktum
import no.nav.dagpenger.opplysning.Behovsløserkilde
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.dagpenger.opplysning.Faktum as FaktumOpplysning

class OpplysningSvarTest {
    private val innhentet = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "innhentet")
    private val grunnlag = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "grunnlag")
    private val periode = Gyldighetsperiode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))

    private fun svar(
        verdi: Boolean = true,
        utledetAv: List<java.util.UUID> = emptyList(),
    ) = OpplysningSvar(
        innhentet,
        verdi,
        Faktum,
        Behovsløserkilde(UUIDv7.ny(), LocalDateTime.now()),
        periode,
        utledetAv,
    )

    @Test
    fun `identisk svar på samme innhenting legges bare til én gang`() {
        val opplysninger = Opplysninger()
        val grunnlagId = FaktumOpplysning(grunnlag, true, periode).also { opplysninger.leggTil(it) }.id

        svar(utledetAv = listOf(grunnlagId)).leggTil(opplysninger).endrerOpplysninger shouldBe true
        svar(utledetAv = listOf(grunnlagId)).leggTil(opplysninger) shouldBe Svarresultat.Duplikat

        opplysninger.finnAlle(innhentet) shouldHaveSize 1
    }

    @Test
    fun `svar med ny verdi er ikke et duplikat`() {
        val opplysninger = Opplysninger()

        svar(verdi = true).leggTil(opplysninger).endrerOpplysninger shouldBe true
        svar(verdi = false).leggTil(opplysninger).endrerOpplysninger shouldBe true
    }

    @Test
    fun `svar basert på grunnlag som ikke lenger er gjeldende ignoreres`() {
        val opplysninger = Opplysninger()

        svar(utledetAv = listOf(UUIDv7.ny())).leggTil(opplysninger) shouldBe Svarresultat.Utdatert

        opplysninger.finnAlle(innhentet) shouldHaveSize 0
    }
}
