package no.nav.dagpenger.avklaring

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.avklaring.TestAvklaringer.ArbeidIEØS
import no.nav.dagpenger.avklaring.TestAvklaringer.TestIkke123
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AvklaringTest {
    @Test
    fun `avklaring må avklares`() {
        val avklaring = Avklaring(ArbeidIEØS)
        avklaring.måAvklares() shouldBe true
        avklaring.kvitter(Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("X123456")), "begrunnelse") shouldBe true
        avklaring.måAvklares() shouldBe false
    }

    @Test
    fun `avklaring i set er unike per kode`() {
        val avklaringer =
            setOf(
                Avklaring(ArbeidIEØS),
                Avklaring(ArbeidIEØS),
                Avklaring(TestIkke123),
                Avklaring(TestIkke123),
            )

        assertEquals(2, avklaringer.size)
    }

    @Test
    fun `endringer sorteres etter tid`() {
        val underBehandling = Avklaring.Endring.UnderBehandling()
        val avklart = Avklaring.Endring.Avklart(avklartAv = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("X123456")))
        val avklaring =
            Avklaring(
                id = UUIDv7.ny(),
                ArbeidIEØS,
                historikk =
                    mutableListOf(
                        avklart,
                        underBehandling,
                    ),
            )
        avklaring.endringer.size shouldBe 2
        val sisteEndring =
            avklaring.endringer
                .last()
        sisteEndring.javaClass.simpleName shouldBe "Avklart"
        avklaring.sistEndret shouldBe sisteEndring.endret

        // Avklaring ikke lenger relevant
        avklaring.avbryt() shouldBe true
        avklaring.endringer.size shouldBe 3
        avklaring.endringer
            .last()
            .javaClass.simpleName shouldBe "Avbrutt"
    }

    @Nested
    inner class DirtyTracking {
        private val saksbehandlerkilde get() = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("X123456"))

        @Test
        fun `ny avklaring har alle endringer som nye`() {
            val avklaring = Avklaring(ArbeidIEØS)

            avklaring.erNy shouldBe true
            avklaring.nyeEndringer shouldHaveSize 1
            avklaring.nyeEndringer.first().shouldBeInstanceOf<Avklaring.Endring.UnderBehandling>()
        }

        @Test
        fun `rehydrert avklaring har ingen nye endringer`() {
            val avklaring =
                Avklaring.rehydrer(
                    UUIDv7.ny(),
                    ArbeidIEØS,
                    mutableListOf(Avklaring.Endring.UnderBehandling()),
                )

            avklaring.erNy shouldBe false
            avklaring.nyeEndringer shouldHaveSize 0
        }

        @Test
        fun `markerLagret nullstiller nye endringer`() {
            val avklaring = Avklaring(ArbeidIEØS)

            avklaring.nyeEndringer shouldHaveSize 1
            avklaring.markerLagret()
            avklaring.nyeEndringer shouldHaveSize 0
            avklaring.erNy shouldBe false
        }

        @Test
        fun `endringer etter markerLagret er nye`() {
            val avklaring = Avklaring(ArbeidIEØS)
            avklaring.markerLagret()

            avklaring.kvitter(saksbehandlerkilde, "ok")
            avklaring.nyeEndringer shouldHaveSize 1
            avklaring.nyeEndringer.first().shouldBeInstanceOf<Avklaring.Endring.Avklart>()
        }

        @Test
        fun `rehydrert avklaring som endres har bare de nye endringene`() {
            val avklaring =
                Avklaring.rehydrer(
                    UUIDv7.ny(),
                    ArbeidIEØS,
                    mutableListOf(Avklaring.Endring.UnderBehandling()),
                )

            avklaring.kvitter(saksbehandlerkilde, "ok")
            avklaring.gjenåpne()

            avklaring.nyeEndringer shouldHaveSize 2
            avklaring.nyeEndringer.map { it::class.simpleName } shouldBe listOf("Avklart", "UnderBehandling")
        }

        @Test
        fun `full livssyklus - opprett, lagre, rehydrer, endre, lagre`() {
            // 1. Opprett ny avklaring
            val avklaring = Avklaring(ArbeidIEØS)
            avklaring.erNy shouldBe true
            avklaring.nyeEndringer shouldHaveSize 1

            // 2. Simuler lagring
            avklaring.markerLagret()
            avklaring.erNy shouldBe false
            avklaring.nyeEndringer shouldHaveSize 0

            // 3. Simuler rehydrering fra DB
            val rehydrert =
                Avklaring.rehydrer(
                    avklaring.id,
                    avklaring.kode,
                    avklaring.endringer.toMutableList(),
                )
            rehydrert.erNy shouldBe false
            rehydrert.nyeEndringer shouldHaveSize 0

            // 4. Gjør endringer på rehydrert objekt
            rehydrert.kvitter(saksbehandlerkilde, "ok")
            rehydrert.nyeEndringer shouldHaveSize 1
            rehydrert.erNy shouldBe false

            // 5. Simuler ny lagring
            rehydrert.markerLagret()
            rehydrert.nyeEndringer shouldHaveSize 0
            rehydrert.endringer shouldHaveSize 2
        }

        @Test
        fun `flere lagringer akkumulerer ikke falske nye endringer`() {
            val avklaring = Avklaring(ArbeidIEØS)

            // Lagre tre ganger uten endringer mellom
            avklaring.markerLagret()
            avklaring.markerLagret()
            avklaring.markerLagret()

            avklaring.nyeEndringer shouldHaveSize 0
            avklaring.endringer shouldHaveSize 1
        }
    }
}
