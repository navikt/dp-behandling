package no.nav.dagpenger.opplysning

import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.regelsett.ReglerForInntektTest
import no.nav.dagpenger.opplysning.regelsett.TestPrøvingsdatoRegelsett
import no.nav.dagpenger.opplysning.verdier.Beløp
import org.junit.jupiter.api.Test

class OpplysningerTombstoneTest {
    @Test
    fun `lagTombstone gjør en arvet opplysning usynlig for oppslag, men beholder den i sporloggen`() {
        val inntekt =
            Faktum(ReglerForInntektTest.inntekt12, Beløp(221221.0), Gyldighetsperiode(1.januar, 1.mai))
        val tidligereBehandling = Opplysninger.med(inntekt)
        val revurdering = Opplysninger.basertPå(tidligereBehandling)

        // Før tombstone: opplysningen er synlig og arvet
        revurdering.har(ReglerForInntektTest.inntekt12) shouldBe true
        revurdering.erArvet(inntekt) shouldBe true

        revurdering.lagTombstone(ReglerForInntektTest.inntekt12)

        // Etter tombstone: opplysningstypen fremstår som manglende
        revurdering.har(ReglerForInntektTest.inntekt12) shouldBe false
        revurdering.finnNullableOpplysning(ReglerForInntektTest.inntekt12) shouldBe null
        revurdering.finnAlle(ReglerForInntektTest.inntekt12) shouldBe emptyList()
        revurdering.finnFlere(listOf(ReglerForInntektTest.inntekt12)) shouldBe emptyList()

        // Den arvede opplysningen finnes fortsatt i sporloggen (den er aldri fjernet eller endret),
        // men markert som tombstonet slik at den ikke lenger telles med i verdi-oppslag
        val original = revurdering.somListe(LesbarOpplysninger.Filter.Alle).single { it.id == inntekt.id }
        original shouldBe inntekt
    }

    @Test
    fun `lagTombstone fjerner egne opplysninger av samme type reelt`() {
        val arvetInntekt =
            Faktum(ReglerForInntektTest.inntekt12, Beløp(100000.0), Gyldighetsperiode(1.januar, 1.mai))
        val tidligereBehandling = Opplysninger.med(arvetInntekt)
        val revurdering = Opplysninger.basertPå(tidligereBehandling)

        val egenInntekt =
            Faktum(ReglerForInntektTest.inntekt12, Beløp(200000.0), Gyldighetsperiode(2.mai, 9.mai))
        revurdering.leggTil(egenInntekt)

        revurdering.lagTombstone(ReglerForInntektTest.inntekt12)

        // Både den egne og den arvede opplysningen skal nå fremstå som manglende
        revurdering.har(ReglerForInntektTest.inntekt12) shouldBe false
        revurdering.finnNullableOpplysning(ReglerForInntektTest.inntekt12) shouldBe null
    }

    @Test
    fun `lagTombstone fører til at regelmotoren ber om ny inntekt`() {
        val tidligereBehandling =
            Opplysninger.med(
                Faktum(ReglerForInntektTest.inntekt12, Beløp(221221.0), Gyldighetsperiode(1.januar, 1.mai)),
                Faktum(ReglerForInntektTest.inntekt36, Beløp(221221.0), Gyldighetsperiode(1.januar, 1.mai)),
            )
        val revurdering = Opplysninger.basertPå(tidligereBehandling)
        val regelkjøring =
            Regelkjøring(
                9.mai,
                revurdering,
                ReglerForInntektTest.regelsett,
                TestPrøvingsdatoRegelsett.regelsett,
            )

        revurdering
            .leggTil(Faktum(TestPrøvingsdatoRegelsett.søknadsdato, 9.mai, Gyldighetsperiode(9.mai)))
            .also { regelkjøring.evaluer() }
        revurdering.leggTil(Faktum(TestPrøvingsdatoRegelsett.sisteDagMedArbeidsplikt, 9.mai)).also { regelkjøring.evaluer() }
        revurdering.leggTil(Faktum(TestPrøvingsdatoRegelsett.sisteDagMedLønn, 9.mai)).also { regelkjøring.evaluer() }

        regelkjøring.evaluer().informasjonsbehov shouldContainKey ReglerForInntektTest.inntekt12

        revurdering.lagTombstone(ReglerForInntektTest.inntekt12)

        regelkjøring.evaluer().informasjonsbehov shouldContainKey ReglerForInntektTest.inntekt12
    }
}
