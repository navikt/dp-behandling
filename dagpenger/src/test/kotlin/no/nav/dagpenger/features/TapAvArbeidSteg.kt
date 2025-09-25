package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.utils.somLocalDate
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.kravTilArbeidstidsreduksjonVedFiskepermittering
import no.nav.dagpenger.regel.ReellArbeidssøker
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Rettighetstype
import no.nav.dagpenger.regel.Rettighetstype.permitteringFiskeforedling
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregningsregel6mnd
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.kravPåLønn
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntekt
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.kravTilTaptArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.nyArbeidstid
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.vernepliktFastsattVanligArbeidstid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class TapAvArbeidSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett = RegelverkDagpenger.regelsettFor(kravTilTaptArbeidstid) + Rettighetstype.regelsett
//        listOf(
//            Rettighetstype.regelsett,
//            TapAvArbeidsinntektOgArbeidstid.regelsett,
//            Søknadstidspunkt.regelsett,
//            PermittertFraFiskeindustrien.regelsett,
//            VernepliktFastsetting.regelsett,
//            Verneplikt.regelsett,
//        ) // + RegelverkDagpenger.regelsettFor(kravTilTaptArbeidstid)

    private val opplysninger = Opplysninger()

    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søknadsdatossssss er {string}") { søknadsdato: String ->
            opplysninger.leggTil(Faktum(Søknadstidspunkt.søknadsdato, søknadsdato.somLocalDate()))
            opplysninger.leggTil(Faktum(Søknadstidspunkt.ønsketdato, søknadsdato.somLocalDate()))
            opplysninger.leggTil(Faktum(ReellArbeidssøker.ønsketArbeidstid, 40.0))

            opplysninger.leggTil(Faktum(grunnlagForVernepliktErGunstigst, false))
            opplysninger.leggTil(Faktum(permitteringFiskeforedling, false))
            opplysninger.leggTil(Faktum(vernepliktFastsattVanligArbeidstid, 0.0))
            opplysninger.leggTil(Faktum(kravTilArbeidstidsreduksjonVedFiskepermittering, 40.0))
        }

        Gitt("at personen har tapt arbeid") { /* NOOP */ }
        Og("personen har tapt arbeidsinntekt") {
            opplysninger.leggTil(Faktum(kravPåLønn, false)).also { regelkjøring.evaluer() }
        }
        Og("har fått fastsatt vanlig arbeidstid til {double}") { timer: Double ->
            opplysninger.leggTil(Faktum(beregnetArbeidstid, timer)).also { regelkjøring.evaluer() }
            opplysninger.leggTil(Faktum(beregningsregel6mnd, true)).also { regelkjøring.evaluer() }
        }
        Og("har ny arbeidstid {double}") { timer: Double ->
            opplysninger.leggTil(Faktum(nyArbeidstid, timer)).also { regelkjøring.evaluer() }
        }
        Og("personen er permittert fra fiskeindustrien {boolsk}") { permittert: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(Rettighetstype.permitteringFiskeforedling, permittert) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Når("personen søker om dagpenger") { }
        Så("skal personen oppfylle kravet til tap av arbeidsinntekt") {
            assertTrue(opplysninger.har(kravTilTapAvArbeidsinntekt))
            assertTrue(opplysninger.finnOpplysning(kravTilTapAvArbeidsinntekt).verdi)
        }
        Og("personen skal {string} kravet til tap av arbeidstid") { utfall: String ->
            val verdi = utfall(utfall)
            assertTrue(opplysninger.har(kravTilTaptArbeidstid))
            assertEquals(opplysninger.finnOpplysning(kravTilTaptArbeidstid).verdi, verdi)
            assertEquals(opplysninger.finnOpplysning(kravTilTapAvArbeidsinntektOgArbeidstid).verdi, verdi)
        }
    }

    private fun utfall(verdi: String) =
        when (verdi) {
            "ikke oppfylt" -> false
            "oppfylt" -> true
            else -> throw IllegalArgumentException("Ukjent utfall")
        }
}
