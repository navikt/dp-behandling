package no.nav.dagpenger.scenario

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.august
import no.nav.dagpenger.mediator.mars
import no.nav.dagpenger.mediator.september
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.deltakelseIArbeidsmarkedstiltak
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.tarUtdanning
import no.nav.dagpenger.scenario.MeldekortAktivitet.Arbeid
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test

class UtdanningTest {
    @Test
    fun `flere meldekort med utdanning i ulike perioder`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(4.august(2026), ønskerFraDato = 10.august(2026))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().fraOgMed shouldBe 10.august(2026)
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(2) {}

            // Med en dag utdanning
            person.sendInnMeldekort(
                2,
                aktiviteter =
                    listOf(
                        Arbeid(7),
                        Arbeid(7),
                        Arbeid(0),
                        Arbeid(7),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(7),
                        Arbeid(0),
                        MeldekortAktivitet.Utdanning(7),
                        MeldekortAktivitet.Utdanning(7),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                    ),
            )
            val meldekortId = meldekortBatch(markerFerdig = false).last()
            // TODO: Når false legges til først blir "Godkjent unntak for utdanning eller opplæring?" aldri revurdert
            // Motsatt rekkefølge hjelper mye
            saksbehandler.endreOpplysning(tarUtdanning, false, gyldighetsperiode = Gyldighetsperiode(27.august(2026)))
            saksbehandler.endreOpplysning(tarUtdanning, true, gyldighetsperiode = Gyldighetsperiode(25.august(2026), 26.august(2026)))
            saksbehandler.endreOpplysning(
                deltakelseIArbeidsmarkedstiltak,
                true,
                gyldighetsperiode = Gyldighetsperiode(25.august(2026), 26.august(2026)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()
            meldekortFerdig(meldekortId)

            behandlingsresultat(3) {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Meldekort uke 38-39
            person.sendInnMeldekort(
                3,
                aktiviteter =
                    listOf(
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        MeldekortAktivitet.Fravær,
                        MeldekortAktivitet.Fravær,
                        MeldekortAktivitet.Fravær,
                        MeldekortAktivitet.Fravær,
                        MeldekortAktivitet.Fravær,
                        Arbeid(0),
                        Arbeid(0),
                    ),
            )
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(4) {}

            // Meldekort uke 38-39
            person.sendInnMeldekort(4)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(5) {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Meldekort uke 40-41 med utdanning
            person.sendInnMeldekort(
                5,
                aktiviteter =
                    listOf(
                        MeldekortAktivitet.Utdanning(0),
                        MeldekortAktivitet.Utdanning(0),
                        MeldekortAktivitet.Utdanning(0),
                        MeldekortAktivitet.Utdanning(0),
                        MeldekortAktivitet.Utdanning(0),
                        Arbeid(0),
                        Arbeid(0),
                        MeldekortAktivitet.Utdanning(0),
                        MeldekortAktivitet.Utdanning(0),
                        MeldekortAktivitet.Utdanning(0),
                        MeldekortAktivitet.Utdanning(0),
                        MeldekortAktivitet.Utdanning(0),
                        Arbeid(0),
                        Arbeid(0),
                    ),
            )
            meldekortBatch(markerFerdig = false)
            saksbehandler.endreOpplysning(tarUtdanning, true, gyldighetsperiode = Gyldighetsperiode(28.september(2026), 27.mars(2027)))
            saksbehandler.endreOpplysning(tarUtdanning, false, gyldighetsperiode = Gyldighetsperiode(28.mars(2027)))
            saksbehandler.endreOpplysning(
                deltakelseIArbeidsmarkedstiltak,
                false,
                gyldighetsperiode = Gyldighetsperiode(27.august(2026), 27.september(2026)),
            )
            saksbehandler.endreOpplysning(
                deltakelseIArbeidsmarkedstiltak,
                true,
                gyldighetsperiode = Gyldighetsperiode(28.september(2026), 27.mars(2027)),
            )

            behandlingsresultatForslag(12) {
                rettighetsperioder.single().harRett shouldBe true
            }
        }
    }
}
