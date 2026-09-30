package no.nav.dagpenger.regel
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.prosess.RettighetsperiodePlugin
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test

class RettighetsperiodePluginTest {
    private val utfall1 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "boolsk")
    private val utfall2 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "boolsk")

    private val regelverk =
        Regelverk(
            RegelverkType("Test"),
            regelsett =
                arrayOf(
                    vilkår("vilkår 2") {
                        utfall(utfall2) { somUtgangspunkt(true) }
                    },
                ),
        )

    private val regelverkMedEtablering =
        Regelverk(
            RegelverkType("Test med etablering"),
            regelsett = (regelverk.regelsett + Etablering.regelsett).toTypedArray(),
        )

    @Test
    fun `godkjent etablering setter til og med uten å flytte fra og med`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(utfall2, true, Gyldighetsperiode(1.januar(2025))),
                Faktum(Rettighetstype.skalEtableringVurderes, true),
                Faktum(Etablering.påvirkerUtfallet, true),
                Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(15.januar(2025), 31.januar(2025))),
            )

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        val periode = opplysninger.finnAlle(harLøpendeRett).single()
        periode.gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2025), 31.januar(2025))
        periode.verdi shouldBe true
    }

    @Test
    fun `godkjent etablering oppdaterer sluttdato på en arvet rettighetsperiode`() {
        val innvilgelse = Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2025)))
        val opplysninger =
            Opplysninger.basertPå(Opplysninger.med(innvilgelse)).apply {
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(Rettighetstype.skalEtableringVurderes, true))
                leggTil(Faktum(Etablering.påvirkerUtfallet, true))
                leggTil(Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(15.januar(2025), 31.januar(2025))))
            }
        val plugin = RettighetsperiodePlugin(regelverkMedEtablering)

        repeat(2) {
            plugin.regelkjøringFerdig(Prosesskontekst(opplysninger))

            val periode = opplysninger.finnAlle(harLøpendeRett).single()
            periode.gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2025), 31.januar(2025))
            periode.verdi shouldBe true
            periode.erstatter shouldBe innvilgelse
        }
    }

    @Test
    fun `godkjent etablering kan forlenge sluttdato uten å flytte starten`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(utfall2, true, Gyldighetsperiode(1.januar(2025), 20.januar(2025))),
                Faktum(Rettighetstype.skalEtableringVurderes, true),
                Faktum(Etablering.påvirkerUtfallet, true),
                Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(15.januar(2025), 31.januar(2025))),
            )

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        val periode = opplysninger.finnAlle(harLøpendeRett).single()
        periode.gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2025), 31.januar(2025))
        periode.verdi shouldBe true
    }

    @ParameterizedTest
    @CsvSource(
        "false, false, false",
        "false, false, true",
        "false, true, false",
        "false, true, true",
        "true, false, false",
        "true, false, true",
        "true, true, false",
    )
    fun `etablering som ikke er relevant og godkjent lar arvet rett være uendret`(
        skalVurderes: Boolean,
        påvirkerUtfallet: Boolean,
        godkjent: Boolean,
    ) {
        val innvilgelse = Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2025), 20.januar(2025)))
        val opplysninger =
            Opplysninger.basertPå(Opplysninger.med(innvilgelse)).apply {
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(1.januar(2025))))
                leggTil(Faktum(Rettighetstype.skalEtableringVurderes, skalVurderes))
                leggTil(Faktum(Etablering.påvirkerUtfallet, påvirkerUtfallet))
                leggTil(Faktum(Etablering.etableringGodkjent, godkjent, Gyldighetsperiode(15.januar(2025), 31.januar(2025))))
            }

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        opplysninger.finnAlle(harLøpendeRett) shouldBe listOf(innvilgelse)
        opplysninger.finnAlle(harLøpendeRett).single().gyldighetsperiode shouldBe innvilgelse.gyldighetsperiode
    }

    @Test
    fun `etablering som ikke er ferdig vurdert påvirker ikke rettighetsperioden`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(utfall2, true, Gyldighetsperiode(1.januar(2025))),
                Faktum(Rettighetstype.skalEtableringVurderes, true),
                Faktum(Etablering.påvirkerUtfallet, true),
            )

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        val periode = opplysninger.finnAlle(harLøpendeRett).single()
        periode.gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2025))
        periode.verdi shouldBe true
    }

    @Test
    fun `godkjent etablering gir ikke rett når øvrige vilkår ikke er oppfylt`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(utfall2, false, Gyldighetsperiode(1.januar(2025))),
                Faktum(Rettighetstype.skalEtableringVurderes, true),
                Faktum(Etablering.påvirkerUtfallet, true),
                Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(15.januar(2025), 31.januar(2025))),
            )

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        val periode = opplysninger.finnAlle(harLøpendeRett).single()
        periode.gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2025))
        periode.verdi shouldBe false
    }

    @Test
    fun `godkjent etablering lager ikke rettighetsperioder når øvrige vilkår mangler`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(Rettighetstype.skalEtableringVurderes, true),
                Faktum(Etablering.påvirkerUtfallet, true),
                Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(15.januar(2025), 31.januar(2025))),
            )

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        opplysninger.finnAlle(harLøpendeRett) shouldHaveSize 0
    }

    @Test
    fun `godkjent etablering overstyrer ikke en senere stans`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(utfall2, true, Gyldighetsperiode(1.januar(2025), 10.januar(2025))),
                Faktum(utfall2, false, Gyldighetsperiode(11.januar(2025))),
                Faktum(Rettighetstype.skalEtableringVurderes, true),
                Faktum(Etablering.påvirkerUtfallet, true),
                Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(5.januar(2025), 31.januar(2025))),
            )

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        opplysninger.finnAlle(harLøpendeRett).map { it.gyldighetsperiode to it.verdi } shouldBe
            listOf(
                Gyldighetsperiode(1.januar(2025), 10.januar(2025)) to true,
                Gyldighetsperiode(11.januar(2025)) to false,
            )
    }

    @Test
    fun `godkjent etablering endrer bare siste rettighetsperiode og bevarer historikken`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(utfall2, true, Gyldighetsperiode(1.januar(2025), 10.januar(2025))),
                Faktum(utfall2, false, Gyldighetsperiode(11.januar(2025), 14.januar(2025))),
                Faktum(utfall2, true, Gyldighetsperiode(15.januar(2025))),
                Faktum(Rettighetstype.skalEtableringVurderes, true),
                Faktum(Etablering.påvirkerUtfallet, true),
                Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(20.januar(2025), 31.januar(2025))),
            )

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        opplysninger.finnAlle(harLøpendeRett).map { it.gyldighetsperiode to it.verdi } shouldBe
            listOf(
                Gyldighetsperiode(1.januar(2025), 10.januar(2025)) to true,
                Gyldighetsperiode(11.januar(2025), 14.januar(2025)) to false,
                Gyldighetsperiode(15.januar(2025), 31.januar(2025)) to true,
            )
    }

    @Test
    fun `etablering i en tidligere periode endrer ikke en senere rettighetsperiode`() {
        val opplysninger =
            Opplysninger.med(
                Faktum(utfall2, true, Gyldighetsperiode(15.januar(2025))),
                Faktum(Rettighetstype.skalEtableringVurderes, true),
                Faktum(Etablering.påvirkerUtfallet, true),
                Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(1.januar(2025), 10.januar(2025))),
            )

        RettighetsperiodePlugin(regelverkMedEtablering).regelkjøringFerdig(Prosesskontekst(opplysninger))

        val periode = opplysninger.finnAlle(harLøpendeRett).single()
        periode.gyldighetsperiode shouldBe Gyldighetsperiode(15.januar(2025))
        periode.verdi shouldBe true
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `etablering beholder starten når retten forlenges fra en tilstøtende arvet periode`(slåSammenLike: Boolean) {
        val opplysninger =
            Opplysninger
                .basertPå(Opplysninger.med(Faktum(harLøpendeRett, true, Gyldighetsperiode(1.januar(2025), 14.januar(2025)))))
                .apply {
                    leggTil(Faktum(utfall2, true, Gyldighetsperiode(15.januar(2025))))
                    leggTil(Faktum(Rettighetstype.skalEtableringVurderes, true))
                    leggTil(Faktum(Etablering.påvirkerUtfallet, true))
                    leggTil(Faktum(Etablering.etableringGodkjent, true, Gyldighetsperiode(20.januar(2025), 31.januar(2025))))
                }

        RettighetsperiodePlugin(regelverkMedEtablering, slåSammenLike = slåSammenLike)
            .regelkjøringFerdig(Prosesskontekst(opplysninger))

        val periode = opplysninger.finnAlle(harLøpendeRett).single()
        periode.gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2025), 31.januar(2025))
        periode.verdi shouldBe true
    }

    @Test
    fun `lager ikke perioder når alle relevant vilkår mangler`() {
        val plugin = RettighetsperiodePlugin(regelverk)
        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(utfall1, true, Gyldighetsperiode(1.januar(2018))))
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(15.januar(2018))))
            }

        // Lag perioder av løpende rett
        plugin.regelkjøringFerdig(Prosesskontekst(opplysninger))

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
        plugin.regelkjøringFerdig(Prosesskontekst(opplysninger))

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
        plugin.regelkjøringFerdig(Prosesskontekst(opplysninger))

        val perioder = opplysninger.finnAlle(harLøpendeRett)

        perioder shouldHaveSize 3
        perioder[0].gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2018), 10.januar(2018))
        perioder[0].verdi shouldBe true

        perioder[1].gyldighetsperiode shouldBe Gyldighetsperiode(11.januar(2018), 14.januar(2018))
        perioder[1].verdi shouldBe false

        perioder[2].gyldighetsperiode shouldBe Gyldighetsperiode(15.januar(2018))
        perioder[2].verdi shouldBe true
    }

    @Test
    fun `slår sammen flere nye kant-i-kant perioder med lik verdi beregnet i samme kjøring`() {
        val plugin = RettighetsperiodePlugin(regelverk, slåSammenLike = false)

        val opplysninger =
            Opplysninger().apply {
                leggTil(Faktum(utfall1, true, Gyldighetsperiode(1.januar(2018))))
                // To separate, kant-i-kant vilkårsopplysninger med lik verdi (true), som ellers ville
                // gitt to distinkte segmenter i tidslinja siden slåSammenLike er skrudd av
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(1.januar(2018), 10.januar(2018))))
                leggTil(Faktum(utfall2, true, Gyldighetsperiode(11.januar(2018))))
            }

        plugin.regelkjøringFerdig(Prosesskontekst(opplysninger))

        val perioder = opplysninger.finnAlle(harLøpendeRett)

        perioder shouldHaveSize 1
        perioder[0].gyldighetsperiode shouldBe Gyldighetsperiode(1.januar(2018))
        perioder[0].verdi shouldBe true
    }
}
