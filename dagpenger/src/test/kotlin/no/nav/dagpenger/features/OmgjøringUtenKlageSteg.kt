package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.RegelkjøringObserver
import no.nav.dagpenger.opplysning.Regelkjøringsrapport
import no.nav.dagpenger.regel.hendelse.OmgjøringHendelse
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import no.nav.dagpenger.regel.prosessvilkår.OmgjøringUtenKlage

class OmgjøringUtenKlageSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(OmgjøringUtenKlage.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring =
            Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray()).also {
                it.leggTilObservatør(
                    object : RegelkjøringObserver {
                        override fun evaluert(
                            rapport: Regelkjøringsrapport,
                            alleOpplysninger: LesbarOpplysninger,
                            aktiveOpplysninger: LesbarOpplysninger,
                        ) {
                            println(rapport)
                        }
                    },
                )
            }
    }

    init {
        Gitt("at vi har startet en omgjøringsprosess") {
            opplysninger
                .leggTil(
                    Faktum(
                        hendelseTypeOpplysningstype,
                        OmgjøringHendelse::class.java.name,
                    ),
                ).also { regelkjøring.evaluer() }
            // Write code here that turns the phrase above into concrete actions
        }
        Gitt("endringen ikke er til skade for noen som vedtaket {boolsk}") { svar: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(
                        OmgjøringUtenKlage.endringIkkeTilSkade,
                        svar,
                    ),
                ).also { regelkjøring.evaluer() }
        }

        Gitt("{boolsk} om vedtaket ikke er kommet fram til vedkommende og vedtaket heller ikke er offentlig kunngjort") { svar: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(
                        OmgjøringUtenKlage.ikkeUnderretning,
                        svar,
                    ),
                ).also { regelkjøring.evaluer() }
        }

        Gitt("vedtaket må anses {boolsk}") { svar: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(
                        OmgjøringUtenKlage.ansesUgyldigVedtak,
                        svar,
                    ),
                ).also { regelkjøring.evaluer() }
        }

        Så("kan forvaltningsorgan {boolsk} sitt eget vedtak uten at det er påklaget") { svar: Boolean ->
            opplysninger.finnOpplysning(OmgjøringUtenKlage.kanOmgjøresUtenKlage).verdi shouldBe svar
        }
    }
}
