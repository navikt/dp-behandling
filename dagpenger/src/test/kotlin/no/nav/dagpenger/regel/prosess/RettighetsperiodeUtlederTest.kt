package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.PeriodisertVerdi
import no.nav.dagpenger.uuid.UUIDv7
import kotlin.test.Test

class RettighetsperiodeUtlederTest {
    private val type = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "harLøpendeRett")

    private fun eksisterende(
        verdi: Boolean,
        gyldighetsperiode: Gyldighetsperiode,
    ): Opplysning<Boolean> = Faktum(type, verdi, gyldighetsperiode)

    @Test
    fun `ingen kandidatperioder gir ingen nye rettighetsperioder`() {
        val nye =
            RettighetsperiodeUtleder.utledNyeRettighetsperioder(
                eksisterende = emptyList(),
                kandidatperioder = emptyList(),
                overskrivingsStrategi = PeriodeOverskrivingsStrategi.BEHOLD_EKSISTERENDE,
            )

        nye shouldBe emptyList()
    }

    @Test
    fun `kandidatperiode uten eksisterende blir en ny rettighetsperiode`() {
        val kandidat = PeriodisertVerdi(1.januar(2018), verdi = true)

        val nye =
            RettighetsperiodeUtleder.utledNyeRettighetsperioder(
                eksisterende = emptyList(),
                kandidatperioder = listOf(kandidat),
                overskrivingsStrategi = PeriodeOverskrivingsStrategi.BEHOLD_EKSISTERENDE,
            )

        nye shouldBe listOf(NyRettighetsperiode(Gyldighetsperiode(1.januar(2018)), true))
    }

    @Test
    fun `kandidatperiode som er kant-i-kant og har lik verdi som forrige eksisterende slås sammen`() {
        val eksisterende = eksisterende(true, Gyldighetsperiode(1.januar(2018), 10.januar(2018)))
        val kandidat = PeriodisertVerdi(11.januar(2018), verdi = true)

        val nye =
            RettighetsperiodeUtleder.utledNyeRettighetsperioder(
                eksisterende = listOf(eksisterende),
                kandidatperioder = listOf(kandidat),
                overskrivingsStrategi = PeriodeOverskrivingsStrategi.BEHOLD_EKSISTERENDE,
            )

        nye shouldBe listOf(NyRettighetsperiode(Gyldighetsperiode(1.januar(2018)), true))
    }

    @Test
    fun `kandidatperiode med ulik verdi enn forrige eksisterende slås ikke sammen`() {
        val eksisterende = eksisterende(false, Gyldighetsperiode(1.januar(2018), 10.januar(2018)))
        val kandidat = PeriodisertVerdi(11.januar(2018), verdi = true)

        val nye =
            RettighetsperiodeUtleder.utledNyeRettighetsperioder(
                eksisterende = listOf(eksisterende),
                kandidatperioder = listOf(kandidat),
                overskrivingsStrategi = PeriodeOverskrivingsStrategi.BEHOLD_EKSISTERENDE,
            )

        nye shouldBe listOf(NyRettighetsperiode(Gyldighetsperiode(11.januar(2018)), true))
    }

    @Test
    fun `BEHOLD_EKSISTERENDE dropper kandidatperiode med samme fraOgMed og verdi som eksisterende`() {
        val eksisterende = eksisterende(true, Gyldighetsperiode(1.januar(2018)))
        val kandidat = PeriodisertVerdi(1.januar(2018), verdi = true)

        val nye =
            RettighetsperiodeUtleder.utledNyeRettighetsperioder(
                eksisterende = listOf(eksisterende),
                kandidatperioder = listOf(kandidat),
                overskrivingsStrategi = PeriodeOverskrivingsStrategi.BEHOLD_EKSISTERENDE,
            )

        nye shouldBe emptyList()
    }

    @Test
    fun `OVERSKRIV_ALLTID legger til kandidatperiode selv om den har samme fraOgMed og verdi som eksisterende`() {
        val eksisterende = eksisterende(true, Gyldighetsperiode(1.januar(2018)))
        val kandidat = PeriodisertVerdi(1.januar(2018), verdi = true)

        val nye =
            RettighetsperiodeUtleder.utledNyeRettighetsperioder(
                eksisterende = listOf(eksisterende),
                kandidatperioder = listOf(kandidat),
                overskrivingsStrategi = PeriodeOverskrivingsStrategi.OVERSKRIV_ALLTID,
            )

        nye shouldBe listOf(NyRettighetsperiode(Gyldighetsperiode(1.januar(2018)), true))
    }

    @Test
    fun `siste kant-i-kant kandidatperiode med lik verdi og åpen sluttdato utvides til å dekke forrige`() {
        // Begge periodene legges til (Opplysninger sitt eget overlapp-baserte "uterstatning"-mekanikk
        // sørger for at den forrige blir overskrevet når den siste utvides til å dekke den),
        // så vi forventer to elementer her, der det siste dekker hele det sammenslåtte tidsrommet.
        val kandidater =
            listOf(
                PeriodisertVerdi(1.januar(2018), 10.januar(2018), true),
                PeriodisertVerdi(11.januar(2018), verdi = true),
            )

        val nye =
            RettighetsperiodeUtleder.utledNyeRettighetsperioder(
                eksisterende = emptyList(),
                kandidatperioder = kandidater,
                overskrivingsStrategi = PeriodeOverskrivingsStrategi.BEHOLD_EKSISTERENDE,
            )

        nye shouldBe
            listOf(
                NyRettighetsperiode(Gyldighetsperiode(1.januar(2018), 10.januar(2018)), true),
                NyRettighetsperiode(Gyldighetsperiode(1.januar(2018)), true),
            )
    }
}
