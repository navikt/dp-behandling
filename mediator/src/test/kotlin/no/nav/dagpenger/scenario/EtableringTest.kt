package no.nav.dagpenger.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.januar
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering.etableringGodkjent
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import java.time.LocalDate
import kotlin.test.Test

class EtableringTest {
    @Test
    fun `etablering påvirker ikke resultatet før den skal vurderes`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(1.januar(2025)) harRett true
                }

                opplysning(Rettighetstype.skalEtableringVurderes) {
                    fra(1.januar(2025)) verdi false
                }

                // Regelsettet har ikke skalKjøres = true ennå, så ingen av opplysningene er produsert
                opplysning(Etablering.nyVirksomhet) {
                    erIkkeVurdert()
                }
            }
        }
    }

    @Test
    fun `etablering påvirker ikke resultatet der en har ikke godkjent etablering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()

            val etableringFra = 10.januar(2025)
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEtableringVurderes,
                false,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(fraOgMed = 1.januar(2025), tilOgMed = etableringFra.minusDays(1)),
            )
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEtableringVurderes,
                true,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(etableringFra),
            )

            listOf(
                Etablering.nyVirksomhet,
                Etablering.selvforsørget,
                Etablering.godkjentNæringsfaglig,
                Etablering.ikkeSelvforskyldtArbeidsledig,
                Etablering.overFemtiProsentEierandel,
            ).forEach { saksbehandler.leggTilOpplysning(it, false, gyldighetsperiode = Gyldighetsperiode(fraOgMed = etableringFra)) }

            saksbehandler.leggTilOpplysning(Etablering.sluttDato, 31.januar(2025))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(1.januar(2025)) harRett true
                }

                opplysning(Rettighetstype.skalEtableringVurderes) {
                    fra(1.januar(2025)) til 9.januar(2025) verdi false
                    fra(etableringFra) verdi true
                }
                opplysning(etableringGodkjent) {
                    erIkkeVurdert()
                }
            }
        }
    }

    @Test
    fun `godkjent etablering setter sluttdato på løpende rett`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.omgjørBehandling(1.januar(2025))
            val etableringFra = 10.januar(2025)
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEtableringVurderes,
                true,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(etableringFra),
            )

            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                // Regelsettet kjøres nå, og standardverdiene (somUtgangspunkt) er produsert
                opplysning(Etablering.nyVirksomhet) {
                    fra(etableringFra) verdi false
                }
                opplysning(Etablering.selvforsørget) {
                    fra(etableringFra) verdi false
                }
                opplysning(Etablering.godkjentNæringsfaglig) {
                    fra(etableringFra) verdi false
                }
                opplysning(Etablering.ikkeSelvforskyldtArbeidsledig) {
                    fra(etableringFra) verdi false
                }
                opplysning(etableringGodkjent) {
                    fra(etableringFra) til LocalDate.now().plusMonths(12).minusDays(1) verdi false
                }

                rettighetsperioder {
                    fra(1.januar(2025)) harRett true
                }
            }

            saksbehandler.leggTilOpplysning(Etablering.sluttDato, 31.januar(2025))
            listOf(
                Etablering.nyVirksomhet,
                Etablering.selvforsørget,
                Etablering.godkjentNæringsfaglig,
                Etablering.ikkeSelvforskyldtArbeidsledig,
                Etablering.overFemtiProsentEierandel,
            ).forEach { saksbehandler.leggTilOpplysning(it, true, gyldighetsperiode = Gyldighetsperiode(fraOgMed = etableringFra)) }

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(2) {
                rettighetsperioder {
                    fra(1.januar(2025)) til 9.januar(2025) harRett true
                    fra(10.januar(2025)) til 31.januar(2025) harRett true
                }
                opplysning(Rettighetstype.skalEtableringVurderes) {
                    fra(1.januar(2025)) til 9.januar(2025) verdi false
                    fra(etableringFra) verdi true
                }
                opplysning(etableringGodkjent) {
                    fra(10.januar(2025)) til 31.januar(2025) verdi true
                }
            }
        }
    }

    @Test
    fun `test stans ved etablering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.omgjørBehandling(1.januar(2025))
            val etableringFra = 10.januar(2025)
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEtableringVurderes,
                false,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(fraOgMed = 1.januar(2025), tilOgMed = etableringFra.minusDays(1)),
            )
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEtableringVurderes,
                true,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(etableringFra),
            )

            listOf(
                Etablering.nyVirksomhet,
                Etablering.selvforsørget,
                Etablering.godkjentNæringsfaglig,
                Etablering.ikkeSelvforskyldtArbeidsledig,
                Etablering.overFemtiProsentEierandel,
            ).forEach { saksbehandler.leggTilOpplysning(it, true, gyldighetsperiode = Gyldighetsperiode(fraOgMed = etableringFra)) }

            saksbehandler.leggTilOpplysning(
                Etablering.sluttDato,
                14.januar(2025),
                gyldighetsperiode = Gyldighetsperiode(fraOgMed = etableringFra),
            )
            saksbehandler.leggTilOpplysning(
                Etablering.etableringGodkjent,
                false,
                gyldighetsperiode = Gyldighetsperiode(fraOgMed = 15.januar(2025)),
            )
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(2) {
                rettighetsperioder {
                    fra(1.januar(2025)) til 9.januar(2025) harRett true
                    fra(10.januar(2025)) til 14.januar(2025) harRett true
                    fra(15.januar(2025)) harRett false
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            var utbetalt = 0
            behandlingsresultat(3) {
                utbetalt = utbetalinger.sumOf { it["utbetaling"].asInt() }
            }
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(4) {
                førteTil shouldBe "Endring"
                rettighetsperioder {
                    fra(1.januar(2025)) til 9.januar(2025) harRett true
                    fra(10.januar(2025)) til 14.januar(2025) harRett true
                    fra(15.januar(2025)) harRett false
                }
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe utbetalt + 2400
                utbetalinger.last()["dato"].asLocalDate() shouldBe 14.januar(2025)
            }
        }
    }
}
