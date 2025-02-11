package no.nav.dagpenger.avklaring

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.avklaring.Kontrollpunkt.Kontrollresultat
import no.nav.dagpenger.avklaring.TestAvklaringer.ArbeidIEØS
import no.nav.dagpenger.avklaring.TestAvklaringer.BeregningsregelForFVA
import no.nav.dagpenger.avklaring.TestAvklaringer.SvangerskapsrelaterteSykepenger
import no.nav.dagpenger.avklaring.TestAvklaringer.TestIkke123
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class KontrollpunktTest {
    companion object {
        private val opplysningstype = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "test")
        private val inntekterInneholderSykepenger =
            Opplysningstype.boolsk(
                Opplysningstype.Id(UUIDv7.ny(), Boolsk),
                "Inntektene innholder sykepenger",
            )
    }

    private fun getOpplysning(verdi: Int) = Faktum(opplysningstype, verdi)

    @Test
    fun `lager kontroller`() {
        val kontrollpunkt =
            Kontrollpunkt(ArbeidIEØS) { opplysninger ->
                opplysninger.har(opplysningstype) && opplysninger.finnOpplysning(opplysningstype).verdi == 123
            }

        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(1.mai(2024), opplysninger)

        with(kontrollpunkt.evaluer(opplysninger)) {
            this shouldBe Kontrollresultat.OK
        }

        opplysninger.leggTil(getOpplysning(123) as Opplysning<*>).also { regelkjøring.evaluer() }
        with(kontrollpunkt.evaluer(opplysninger)) {
            this.shouldBeInstanceOf<Kontrollresultat.KreverAvklaring>()

            this.avklaringkode shouldBe ArbeidIEØS
        }
    }

    @Test
    fun `avklaringer gjenåpnes når grunnlaget endres`() {
        val kontrollpunkter =
            listOf(
                Kontrollpunkt(TestIkke123) { opplysninger ->
                    opplysninger.har(opplysningstype) && opplysninger.finnOpplysning(opplysningstype).verdi == 321
                },
            )

        val ding = Avklaringer(kontrollpunkter)
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(1.mai(2024), opplysninger).also { it.registrer(ding) }
        opplysninger.leggTil(getOpplysning(321))
        regelkjøring.evaluer()

        ding.måAvklares().also { avklaringer ->
            avklaringer.size shouldBe 1
            avklaringer.all { it.måAvklares() } shouldBe true
            avklaringer.all { it.kode == TestIkke123 } shouldBe true
        }

        // Saksbehandler endrer opplysningen
        opplysninger.leggTil(getOpplysning(123) as Opplysning<*>).also { regelkjøring.evaluer() }

        ding.måAvklares().also { avklaringer ->
            avklaringer.size shouldBe 0
        }

        // Opplysningen endres tilbake til tilstand som krever avklaring
        val endretOpplysning = getOpplysning(321)
        opplysninger.leggTil(endretOpplysning as Opplysning<*>).also { regelkjøring.evaluer() }

        ding.måAvklares().also { avklaringer ->
            avklaringer.size shouldBe 1
            avklaringer.all { it.måAvklares() } shouldBe true
            avklaringer.all { it.kode == TestIkke123 } shouldBe true
            avklaringer.first().sistEndret.shouldBeAfter(endretOpplysning.opprettet)
        }
    }

    @Test
    fun `avklaringer av sykepenger i inntekt`() {
        val kontrollpunkter =
            listOf(
                // Om inntektene inneholder sykepenger, må det avklares om de er svangerskapsrelaterte
                Kontrollpunkt(SvangerskapsrelaterteSykepenger) { opplysninger ->
                    opplysninger.finnOpplysning(inntekterInneholderSykepenger).verdi
                },
            )

        val ding = Avklaringer(kontrollpunkter)
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(1.mai(2024), opplysninger).also { it.registrer(ding) }
        opplysninger.leggTil(Faktum(inntekterInneholderSykepenger, true)).also { regelkjøring.evaluer() }

        ding.måAvklares().also { avklaringer ->
            avklaringer.size shouldBe 1
            avklaringer.all { it.måAvklares() } shouldBe true
            avklaringer.all { it.kode == SvangerskapsrelaterteSykepenger } shouldBe true
        }

        // Saksbehandler kvittererer ut avklaringen fordi sykepengene ikke er svangerskapsrelaterte
        ding.avklaringer
            .first()
            .kvitter(
                Saksbehandlerkilde(
                    UUIDv7.ny(),
                    Saksbehandler("Z123456"),
                ),
                "begrunnelse",
            ).also { kvittert ->
                kvittert shouldBe true
            }

        // Nå skal det ikke være avklaringer som må avklares
        ding.måAvklares().also { avklaringer ->
            avklaringer.size shouldBe 0
        }
    }

    @Test
    fun `avklaringer av beregningsregel for fastsatt vanlig arbeidstid`() {
        val regel1 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "6mnd")
        val regel2 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "12mnd")
        val regel3 = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "36mnd")

        val kontrollpunkter =
            listOf(
                // Minst en beregningsregel må settes for fastsatt vanlig arbeidstid
                Kontrollpunkt(BeregningsregelForFVA) { opplysninger ->
                    if (opplysninger.mangler(regel1) || opplysninger.mangler(regel2) || opplysninger.mangler(regel3)) {
                        return@Kontrollpunkt false
                    }
                    listOf(
                        opplysninger.finnOpplysning(regel1),
                        opplysninger.finnOpplysning(regel2),
                        opplysninger.finnOpplysning(regel3),
                    ).count { it.verdi } != 1
                },
            )

        val ding = Avklaringer(kontrollpunkter)
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(1.mai(2024), opplysninger).also { it.registrer(ding) }

        opplysninger.leggTil(Faktum(regel1, false) as Opplysning<*>).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(regel2, false) as Opplysning<*>).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(regel3, false) as Opplysning<*>).also { regelkjøring.evaluer() }

        ding.måAvklares().also { avklaringer ->
            avklaringer.size shouldBe 1
            avklaringer.all { it.måAvklares() } shouldBe true
            avklaringer.all { it.kode == BeregningsregelForFVA } shouldBe true
        }

        opplysninger.leggTil(Faktum(regel1, true)).also { regelkjøring.evaluer() }

        // Denne avklaringen skal ikke kunne kvitteres ut, den krever endring
        shouldThrow<IllegalArgumentException> {
            ding.avklaringer.first().kvitter(Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("Z123456")), "begrunnelse")
        }

        // Nå skal det ikke være avklaringer som må avklares
        ding.måAvklares().also { avklaringer ->
            avklaringer.size shouldBe 0
        }
    }
}
