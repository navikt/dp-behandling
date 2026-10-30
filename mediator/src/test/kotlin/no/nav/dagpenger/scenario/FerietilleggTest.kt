package no.nav.dagpenger.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.ferietillegg.FerietilleggBeløp
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg
import no.nav.dagpenger.mediator.januar
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.mediator.mai
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FerietilleggTest {
    @Test
    fun `tester ferietillegg`() {
        val fnr = "12345678901"
        val ferietilleggId = UUIDv7.ny()
        val opptjeningsår = 2018
        nyttScenario {
            ident = fnr
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            sendFerietillegg(fnr, ferietilleggId, opptjeningsår)
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                opplysning(FerietilleggBeløp.ferietilleggBeløp) {
                    fra(LocalDate.now()) verdi Beløp(47500)
                }
                opplysning(KravPåFerietillegg.harKravpåFerietillegg) {
                    fra(LocalDate.now()) verdi true
                }
            }
        }
    }

    @Test
    fun `tester kjeding av ferietillegg`() {
        val fnr = "12345678901"
        val ferietilleggId = UUIDv7.ny()
        val opptjeningsår = 2018
        nyttScenario {
            ident = fnr
            inntektSiste12Mnd = 500000
        }.test {
            // vi trenger at personen har en innvilget søknad om dagpenger
            person.søkDagpenger(21.juni(2018))
            val dagpengerBehandlingId = person.behandlingId
            behovsløsere.løsTilForslag()
            person.behandling.basertPå shouldBe null

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Vi lager en ferietillegg behandling som bruker den første behandlingen for beregning men kjeder seg ikke på den
            sendFerietillegg(fnr, ferietilleggId, opptjeningsår)
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            val ferietilleggBehandlingId = person.behandlingId

            behandlingsresultat {
                opplysning(KravPåFerietillegg.harKravpåFerietillegg) {
                    fra(LocalDate.now()) verdi true
                }
                opplysning(KravPåFerietillegg.antallDagerForbruk) {
                    fra(1.januar(opptjeningsår)) verdi 100
                }
                opplysning(FerietilleggBeløp.ferietilleggBeløp) {
                    fra(LocalDate.now()) verdi Beløp(47500)
                }
                opplysning(FerietilleggBeløp.sumUtbetaltForÅr) {
                    fra(1.januar(opptjeningsår)) verdi Beløp(500000)
                }
                basertPå shouldBe null

                utbetalinger.size() shouldBe 1
                with(utbetalinger.first()) {
                    this["utbetaling"].asInt() shouldBe 47500
                    this["dato"].asLocalDate() shouldBe 1.mai(2019)
                    this["dagpengeType"].asString() shouldBe "Ferietillegg"
                    this["meldeperiode"].asString() shouldBe "Ferietillegg-2018"
                    this["sats"].asInt() shouldBe 47500
                    this["opprinnelse"].asString() shouldBe "Ny"
                }
            }

            // Nå har vi to kjeder, en med vanlig dagpenger og en ferietillegg
            // Vi sender inn et meldekort som skal kjede seg på dagpenger behandlingen
            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            person.behandling.basertPå shouldBe dagpengerBehandlingId

            // Vi lager en ferietillegg behandling til som baserer seg på den forrige ferietillegg
            sendFerietillegg(fnr, UUIDv7.ny(), opptjeningsår)

            val antallDagerForbruk = 39
            løsBehovForAntallForbruksdager(antallDagerForbruk)
            behovsløsere.løsTilForslag()
            saksbehandler.åpneAvklaringer().filter { it.kode == "FerietilleggRevurdert" }.size shouldBe 1
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat(4) {
                opplysning(KravPåFerietillegg.harKravpåFerietillegg) {
                    fra(LocalDate.now()) verdi false
                }
                opplysning(KravPåFerietillegg.antallDagerForbruk) {
                    fra(1.januar(opptjeningsår)) verdi 39
                }
                opplysning(FerietilleggBeløp.ferietilleggBeløp) {
                    fra(LocalDate.now()) verdi Beløp(0)
                }
                opplysning(FerietilleggBeløp.sumUtbetaltForÅr) {
                    fra(1.januar(opptjeningsår)) verdi Beløp(500000)
                }
                basertPå shouldBe ferietilleggBehandlingId

                utbetalinger.size() shouldBe 1
                with(utbetalinger.first()) {
                    this["utbetaling"].asInt() shouldBe 0
                    this["dato"].asLocalDate() shouldBe 1.mai(2019)
                    this["dagpengeType"].asString() shouldBe "Ferietillegg"
                    this["meldeperiode"].asString() shouldBe "Ferietillegg-2018"
                    this["sats"].asInt() shouldBe 0
                    this["opprinnelse"].asString() shouldBe "Ny"
                }
            }
        }
    }
}
