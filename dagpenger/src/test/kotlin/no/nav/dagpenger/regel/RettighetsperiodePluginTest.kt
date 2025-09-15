package no.nav.dagpenger.regel

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.uuid.UUIDv7
import kotlin.test.Test

class RettighetsperiodePluginTest {
    private val utfall1 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "boolsk")
    private val utfall2 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "boolsk")

    private val regelverk =
        Regelverk(
            vilkår("vilkår 1") {
                utfall(utfall1) { somUtgangspunkt(true) }
            },
            vilkår("vilkår 2") {
                utfall(utfall2) { somUtgangspunkt(true) }
            },
        )

    @Test
    fun `lager ikke perioder når alle relevant vilkår mangler`() {
        val plugin = RettighetsperiodePlugin(regelverk)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(utfall1, true, Gyldighetsperiode(1.januar(2018))))
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(15.januar(2018))))
            }

        // Lag perioder av løpende rett
        plugin.ferdig(opplysninger)

        val perioder = opplysninger.finnAlle(harLøpendeRett)

        perioder shouldHaveSize 1
        perioder[0].gyldighetsperiode shouldBe Gyldighetsperiode(15.januar(2018))
        perioder[0].verdi shouldBe true
    }

    @Test
    fun `lager bare perioder når alle relevant vilkår er vurdert `() {
        val plugin = RettighetsperiodePlugin(regelverk)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(utfall1, true, Gyldighetsperiode(1.januar(2018))))
                leggTil(Faktum(utfall2, false, Gyldighetsperiode(1.januar(2018), 14.januar(2018))))
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(15.januar(2018))))
            }

        // Lag perioder av løpende rett
        plugin.ferdig(opplysninger)

        val perioder = opplysninger.finnAlle(harLøpendeRett)

        perioder shouldHaveSize 2
        perioder[0].gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2018), 14.januar(2018))
        perioder[0].verdi shouldBe false

        perioder[1].gyldighetsperiode shouldBe Gyldighetsperiode(15.januar(2018))
        perioder[1].verdi shouldBe true
    }

    @Test
    fun `lager riktige perioder`() {
        val plugin = RettighetsperiodePlugin(regelverk)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(utfall1, true, Gyldighetsperiode(1.januar(2018))))
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(1.januar(2018), 10.januar(2018))))
                leggTil(Faktum(utfall2, false, Gyldighetsperiode(11.januar(2018), 14.januar(2018))))
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(15.januar(2018))))
            }

        // Lag perioder av løpende rett
        plugin.ferdig(opplysninger)

        val perioder = opplysninger.finnAlle(harLøpendeRett)

        perioder shouldHaveSize 3
        perioder[0].gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2018), 10.januar(2018))
        perioder[0].verdi shouldBe true

        perioder[1].gyldighetsperiode shouldBe Gyldighetsperiode(11.januar(2018), 14.januar(2018))
        perioder[1].verdi shouldBe false

        perioder[2].gyldighetsperiode shouldBe Gyldighetsperiode(15.januar(2018))
        perioder[2].verdi shouldBe true
    }
}
