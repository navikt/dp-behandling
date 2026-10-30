package no.nav.dagpenger.scenario

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.dagpenger.mediator.januar
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.mediator.mai
import no.nav.dagpenger.mediator.mars
import no.nav.dagpenger.mediator.november
import no.nav.dagpenger.opplysning.Avgjørelse.Endring
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Avklaringspunkter.SøknadBehandletTidligere
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.bruktBeregningsregel
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.antallBarn
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.ordinærPeriode
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.regelsett.fastsetting.VernepliktFastsetting.vernepliktPeriode
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.fødselsdato
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold.oppholdINorge
import no.nav.dagpenger.regel.regelsett.vilkår.Opptjeningstid
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering.godkjentPermitteringsårsak
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.kanJobbeHvorSomHelst
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalReellArbeidssøkerVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.samordnetArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.ønsketdato
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregningsregel12mnd
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregningsregel6mnd
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.kravPåLønn
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.kravTilTaptArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.nyArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.deltakelseIArbeidsmarkedstiltak
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.scenario.assertions.Opplysningsperiode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

class ScenarioTest {
    @Test
    fun `tester avslag ved for høy alder`() {
        nyttScenario {
            fødselsdato = 1.mars(1930)
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett false
                }
                opplysning(Alderskrav.kravTilAlder) {
                    fra(21.juni(2018)) verdi false
                }
                opplysning(Minsteinntekt.minsteinntekt) {}

                opplysninger shouldHaveSize 25
            }
        }
    }

    @Test
    fun `tester avslag ved for lite inntekt`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            // Vi sender ikke lenger vedtak_fattet for avslag, siden avslag ikke synkes til Arena
            (0..<rapidInspektør.size).map { rapidInspektør.message(it)["@event_name"].asString() } shouldNotContain "vedtak_fattet"

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett false
                }
                opplysning(Alderskrav.kravTilAlder) {
                    fra(21.juni(2018)) verdi true
                }
                opplysning(Minsteinntekt.minsteinntekt) {
                    fra(21.juni(2018)) verdi false
                }
                opplysning(ReellArbeidssøker.kravTilArbeidssøker) {
                    fra(21.juni(2018)) verdi true
                }

                opplysninger shouldHaveSize 71
            }
        }
    }

    @Test
    fun `tester avslag ved for lite inntekt uten å vurdere reell arbeidssøker`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
            kanJobbeDeltid = false
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            // Avklaring om reell arbeidssøker dukker opp
            with(saksbehandler.åpneAvklaringer()) {
                map { it.kode } shouldContain "ReellArbeidssøkerUnntak"
                this shouldHaveSize 7
            }

            saksbehandler.leggTilOpplysning(skalReellArbeidssøkerVurderes, false)
            with(saksbehandler.lukkAlleAvklaringer()) {
                // Avklaringen for reell arbeidssøker forsvinner automatisk om det ikke skal vurderes
                map { it.kode } shouldNotContain "ReellArbeidssøkerUnntak"
                this shouldHaveAtMostSize 6
            }
            saksbehandler.godkjenn()

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett false
                }
                opplysning(Alderskrav.kravTilAlder) {
                    fra(21.juni(2018)) verdi true
                }
                opplysning(Minsteinntekt.minsteinntekt) {
                    fra(21.juni(2018)) verdi false
                }
                opplysning(ReellArbeidssøker.kravTilArbeidssøker) {}

                opplysninger shouldHaveSize 56
            }
        }
    }

    @Test
    fun `tester innvilgelse`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett true
                }
                opplysning(fastsattVanligArbeidstid) {
                    fra(21.juni(2018)) verdi 37.5
                }
                opplysning(dagsatsEtterSamordningMedBarnetillegg) {
                    fra(21.juni(2018)) verdi Beløp(1242)
                }
                opplysning(antallStønadsdager) {
                    fra(21.juni(2018)) verdi 520
                }
                opplysning(bruktBeregningsregel) {
                    fra(21.juni(2018)) verdi "Inntekt etter avkortning og oppjustering siste 12 måneder"
                }
            }
        }
    }

    @Test
    fun `tester innvilgelse med forskjøvet innvilgelse fram i tid`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                opplysning(Opptjeningstid.sisteAvsluttendendeKalenderMåned) {
                    fra(21.juni(2018)) verdi 31.mai(2018)
                }
            }
            saksbehandler.leggTilOpplysning(
                ønsketdato,
                7.juli(2018),
                "Ønsker innvilgelse fra 7. juli. Endrer opptjeningstidperiode.",
                Gyldighetsperiode(7.juli(2018)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder {
                    fra(7.juli(2018)) harRett true
                }
                opplysning(Opptjeningstid.sisteAvsluttendendeKalenderMåned) {
                    fra(7.juli(2018)) verdi 30.juni(2018)
                }
            }
        }
    }

    @Test
    fun `tester innvilgelse med kjent til og med dato`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                oppyllerKravTilRegistrertArbeidssøker,
                true,
                gyldighetsperiode = Gyldighetsperiode(21.juni(2018), 27.juni(2018)),
            )
            saksbehandler.leggTilOpplysning(
                oppyllerKravTilRegistrertArbeidssøker,
                false,
                gyldighetsperiode = Gyldighetsperiode(28.juni(2018)),
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2018)) til 27.juni(2018) harRett true
                    fra(28.juni(2018)) harRett false
                }
                opplysning(fastsattVanligArbeidstid) {
                    fra(21.juni(2018)) verdi 37.5
                }
                opplysning(dagsatsEtterSamordningMedBarnetillegg) {
                    fra(21.juni(2018)) verdi Beløp(1242)
                }
                opplysning(bruktBeregningsregel) {
                    fra(21.juni(2018)) verdi "Inntekt etter avkortning og oppjustering siste 12 måneder"
                }
            }
        }
    }

    // TODO: Lag en dedikert test som verifiserer at behandlingsresultat har ident til saksbehandler og beslutter
    // TODO: Lag en dedikert test som verifiserer at behandlingsresultat om behandlingen er automatisk

    @Test
    fun `tester innvilgelse ved verneplikt`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
            verneplikt = true
        }.test {
            person.søkDagpenger(21.juni(2021))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2021)) harRett true
                }
                opplysning(dagpengegrunnlag) {
                    fra(21.juni(2021)) verdi Beløp(52490)
                }
                opplysning(grunnlag) {
                    fra(21.juni(2021)) verdi Beløp(319197)
                }
                opplysning(fastsattVanligArbeidstid) {
                    fra(21.juni(2021)) verdi 37.5
                }
                opplysning(dagsatsEtterSamordningMedBarnetillegg) {
                    fra(21.juni(2021)) verdi Beløp(766)
                }
                opplysning(ordinærPeriode) {
                    fra(21.juni(2021)) verdi 0
                }
                opplysning(antallStønadsdager) {
                    fra(21.juni(2021)) verdi 0
                }
                opplysning(vernepliktPeriode) {
                    fra(21.juni(2021)) verdi 26
                }
            }
        }
    }

    @Test
    fun `tester innvilgelse ved permittering fiskeforedling med mange enkeltstående perioder`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                godkjentPermitteringsårsak,
                true,
                "Båten suser",
                Gyldighetsperiode(21.juni(2018), 22.juni(2018)),
            )
            saksbehandler.leggTilOpplysning(
                godkjentPermitteringsårsak,
                false,
                "Båten suser",
                Gyldighetsperiode(23.juni(2018), 26.juni(2018)),
            )
            saksbehandler.leggTilOpplysning(
                godkjentPermitteringsårsak,
                true,
                "Båten suser",
                Gyldighetsperiode(27.juni(2018), 29.juni(2018)),
            )
            saksbehandler.leggTilOpplysning(
                godkjentPermitteringsårsak,
                false,
                "Båten suser",
                Gyldighetsperiode(30.juni(2018), 2.juli(2018)),
            )
            saksbehandler.leggTilOpplysning(godkjentPermitteringsårsak, true, "Båten suser", Gyldighetsperiode(3.juli(2018), 3.juli(2018)))
            saksbehandler.leggTilOpplysning(godkjentPermitteringsårsak, false, "Båten suser", Gyldighetsperiode(4.juli(2018), 5.juli(2018)))
            saksbehandler.leggTilOpplysning(godkjentPermitteringsårsak, true, "Båten suser", Gyldighetsperiode(6.juli(2018), 6.juli(2018)))
            saksbehandler.leggTilOpplysning(godkjentPermitteringsårsak, false, "Båten suser", Gyldighetsperiode(7.juli(2018)))

            behandlingsresultatForslag {
                opplysninger(godkjentPermitteringsårsak)
                rettighetsperioder shouldHaveSize 8
            }
        }
    }

    @Test
    fun `innvilgelse der en er permittering fra fiskeforedling OG tillegg søker om dagpenger etter verneplikt`() {
        nyttScenario {
            inntektSiste12Mnd = 10
            permittertfraFiskeforedling = true
            verneplikt = true
        }.test {
            person.søkDagpenger(21.juni(2018), ønskerFraDato = 22.juni(2018))

            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(1) {
                rettighetsperioder {
                    fra(22.juni(2018)) harRett true
                }
                opplysning(Rettighetstype.skalPermitteringFiskeforedlingVurderes) {
                    fra(21.juni(2018)) verdi true
                }
                opplysning(ordinærPeriode) {
                    fra(22.juni(2018)) verdi 0
                }
                opplysning(vernepliktPeriode) {
                    fra(22.juni(2018)) verdi 26
                }
            }
        }
    }

    @Test
    fun `Fjerning og redigering av opplysninger`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            val opplysning = saksbehandler.fjernOpplysning(fødselsdato)
            person.behandling.harOpplysning(opplysning.perioder.last().id) shouldBe false
        }
    }

    @Test
    fun `tester avslag ved for lite inntekt, ikke reell arbeidssøker, med avslag fra ønsket dato`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2018), 25.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(skalReellArbeidssøkerVurderes, false, "Kan ikke vurdere reell arbeidssøker")
            saksbehandler.leggTilOpplysning(
                prøvingsdato,
                25.juni(2018),
                "Avslag skal være fra ønsøknadsdato",
                Gyldighetsperiode(25.juni(2017)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                rettighetsperioder {
                    fra(25.juni(2018)) harRett false
                }
                opplysning(Alderskrav.kravTilAlder) {
                    fra(21.juni(2018)) verdi true
                }
                opplysning(Minsteinntekt.minsteinntekt) {
                    fra(25.juni(2018)) verdi false
                }
                opplysning(ReellArbeidssøker.kravTilArbeidssøker) {}

                opplysninger shouldHaveSize 56
            }
        }
    }

    @Test
    fun `perioder som legges til i samme behandling og er større en eksisterende erstatter de som ligger der`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(27.november(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.lagBehandling(27.november(2018))
            saksbehandler.leggTilOpplysning(
                oppfyllerKravetTilVerneplikt,
                false,
                "False for alltid",
                Gyldighetsperiode(),
            )
            saksbehandler.leggTilOpplysning(
                oppfyllerKravetTilVerneplikt,
                true,
                "True for alltid",
                Gyldighetsperiode(),
            )

            behandlingsresultatForslag {
                with(opplysninger(oppfyllerKravetTilVerneplikt)) {
                    this.map { it.gyldigFraOgMed }.shouldHaveSize(1)
                }
            }
        }
    }

    @Test
    fun `greier vi gjør for å lage vedtak bak i tid`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(27.november(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                søknadIdOpplysningstype,
                person.sisteSøknadId!!.toString(),
                "Søkte for lenge siden",
                Gyldighetsperiode(1.juni(2018)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                ønsketdato,
                1.juni(2018),
                "Søkte for lenge siden",
                Gyldighetsperiode(1.juni(2018)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                oppyllerKravTilRegistrertArbeidssøker,
                true,
                "Søkte for lenge siden",
                Gyldighetsperiode(1.juni(2018)),
            )
            behovsløsere.løsTilForslag()

            // Manuelt sett prøvingsdato tilbake i tid
            saksbehandler.leggTilOpplysning(
                prøvingsdato,
                1.juni(2018),
                "Søkte for lenge siden",
                Gyldighetsperiode(1.juni(2018)),
            )
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(5) {
                rettighetsperioder {
                    fra(1.juni(2018)) harRett true
                }
            }
        }
    }

    @Test
    @Disabled
    fun `sjekk at vi ikke går i loop når det kommer opplysninger som ikke har avhengighetene sine i samme gyldighetsperiode`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(27.november(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                søknadIdOpplysningstype,
                person.sisteSøknadId!!.toString(),
                "Søkte for lenge siden",
                Gyldighetsperiode(1.juni(2018)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                antallBarn,
                22,
                "Fikk en haug med barn",
                Gyldighetsperiode(5.juni(2018)),
            )

            behandlingsresultatForslag(3) {
                rettighetsperioder {
                    fra(27.november(2018)) harRett true
                }

                with(opplysninger(antallBarn)) {
                    this shouldHaveSize 1
                }
            }
        }
    }

    @Test
    fun `automatisk flytting av prøvingsdato`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2021))
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                opplysning(prøvingsdato) {
                    fra(21.juni(2021)) verdi 21.juni(2021)
                }
            }

            saksbehandler.endreOpplysning(
                ReellArbeidssøker.kravTilArbeidssøker,
                true,
                "Flytter prøvingsdato automatisk",
                Gyldighetsperiode(25.juni(2021)),
            )
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                opplysning(prøvingsdato) {
                    fra(25.juni(2021)) verdi 25.juni(2021)
                }
            }

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()
        }
    }

    @Test
    fun `fang opp mulige hendelser om samordning`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            // Fanger ikke opp meldinger til mennesker uten historikk
            person.fåAnnenYtelse(21.juni(2021))
            rapidInspektør.size shouldBe 0

            // Send søknad
            person.søkDagpenger(21.juni(2021))

            // Melding om mulig samordning fanges ikke opp før behandlingen er ferdig
            person.fåAnnenYtelse(21.juni(2021))

            // Fatt vedtak
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            lateinit var innvilgelseId: UUID
            behandlingsresultat(1) {
                innvilgelseId = behandlingId
                rettighetsperioder {
                    fra(21.juni(2021)) harRett true
                }
            }

            // Meldinger om mulig samordning blir opprettet som en manuell behandling med avklaring
            person.fåAnnenYtelse(24.juni(2021), "SYK")
            behandlingsresultatForslag(3) {
                behandlingId shouldNotBe innvilgelseId
                rettighetsperioder shouldHaveSize 1
            }

            with(saksbehandler.åpneAvklaringer().single()) {
                kode shouldBe "MuligSamordning"
                beskrivelse shouldContain "SYK"
            }
        }
    }

    @Disabled
    @Test
    fun `samordnet arbeidstid skal ikke påvirke fastsatt vanlig arbeidstid når ingenting skal samordnes`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()

            behandlingsresultatForslag {
                // Ingen ytelser er registrert, så det skal ikke samordnes
                opplysning(fastsattVanligArbeidstid) {
                    fra(21.juni(2018)) verdi 37.5
                }
            }

            // Saksbehandler har likevel satt en samordnet arbeidstid (f.eks. lagt igjen fra en tidligere vurdering)
            saksbehandler.leggTilOpplysning(samordnetArbeidstid, 10.0)
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                // Bug: samordnetArbeidstid trekkes fra fastsatt vanlig arbeidstid selv om det ikke skal samordnes
                opplysning(fastsattVanligArbeidstid) {
                    fra(21.juni(2018)) verdi 37.5
                }
            }
        }
    }

    @Test
    fun `ikke lag samordning uten minst en løpende rett`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            // Fatt avslag
            person.søkDagpenger(21.juni(2021))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(21.juni(2021)) harRett false
                }
            }

            // Melding om samordning fører *ikke* til nytt forslag
            person.fåAnnenYtelse(24.juni(2021), "SYK")

            shouldThrow<IllegalArgumentException> {
                behandlingsresultatForslag(3) { }
            }
        }
    }

    @Test
    fun `framprovoserer problemer med perioder med hull som tar over hele greia`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(oppholdINorge, true, gyldighetsperiode = Gyldighetsperiode(1.juni(2018), 10.juni(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.leggTilOpplysning(oppholdINorge, true, gyldighetsperiode = Gyldighetsperiode(20.juni(2018), 30.juni(2018)))
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(3) {
                opplysninger(oppholdINorge) shouldHaveSize 2
            }

            saksbehandler.leggTilOpplysning(kanJobbeHvorSomHelst, true, gyldighetsperiode = Gyldighetsperiode(1.juni(2018), 8.juni(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.leggTilOpplysning(kanJobbeHvorSomHelst, true, gyldighetsperiode = Gyldighetsperiode(11.juni(2018), 12.juni(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.leggTilOpplysning(kanJobbeHvorSomHelst, true, gyldighetsperiode = Gyldighetsperiode(15.juni(2018)))
            behovsløsere.løsTilForslag()

            saksbehandler.fjernOpplysning(kanJobbeHvorSomHelst)
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(7) {
                // Det er nå en ekstra periode mellom 10 og 20. juni som ikke var nødvendig før det ble lagt til instanser av kanJobbeHvorSomHelst i hullet
                opplysninger(oppholdINorge) shouldHaveSize 1

                opplysninger(kanJobbeHvorSomHelst) shouldHaveSize 2
            }
        }
    }

    @Test
    fun `endring opphold til ja i framtiden, da bør det bli nytt innvilgelsestidspunkt`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.endreOpplysning(
                oppholdINorge,
                begrunnelse = "Endres",
                gyldighetsperiode = Gyldighetsperiode(fraOgMed = 10.juni(2018)),
            )
            behovsløsere.løsTilForslag() shouldHaveSize 9

            behandlingsresultatForslag(2) {
                opplysning(oppholdINorge) {
                    fra(10.juni(2018)) verdi true
                }
                rettighetsperioder {
                    fra(10.juni(2018)) harRett true
                }
            }
        }
    }

    @Test
    fun `setter eksplisitt opphold til nei, så ja`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                oppholdINorge,
                false,
                "Endres",
                Gyldighetsperiode(fraOgMed = 1.juni(2018), tilOgMed = 9.juni(2018)),
            )
            behovsløsere.løsTilForslag() shouldHaveSize 0

            saksbehandler.leggTilOpplysning(
                oppholdINorge,
                true,
                "Endres",
                Gyldighetsperiode(fraOgMed = 10.juni(2018)),
            )
            behovsløsere.løsTilForslag() shouldHaveSize 9

            behandlingsresultatForslag(3) {
                opplysning(oppholdINorge) {
                    fra(1.juni(2018)) til 9.juni(2018) verdi false
                    fra(10.juni(2018)) verdi true
                }
                rettighetsperioder {
                    fra(10.juni(2018)) harRett true
                }
            }
        }
    }

    // @Disabled
    @Test
    fun `Bug - endre prøvingsdato til tidligere enn ønsket fra dato ender opp i loop`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(13.mai(2026), 27.mai(2026))
            behovsløsere.løsTilForslag()

            val flyttePrøvingsdato = 15.mai(2026)
            saksbehandler.leggTilOpplysning(
                prøvingsdato,
                flyttePrøvingsdato,
                gyldighetsperiode = Gyldighetsperiode(fraOgMed = flyttePrøvingsdato),
            )
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                rettighetsperioder {
                    fra(15.mai(2026)) harRett true
                }
            }
        }
    }

    @Test
    fun `tester stans og gjenopptak på samme dag`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder shouldHaveSize 1
            }

            person.avsluttArbeidssøkerperiode(22.juni(2018), 22.juni(2018).atTime(14, 2))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat(2) {
                rettighetsperioder shouldHaveSize 2
            }

            person.søkGjenopptak(22.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.åpneAvklaringer().map { it.kode } shouldContainAll listOf("GjenopptakBehandling")

            behandlingsresultatForslag(5) {
                rettighetsperioder {
                    fra(21.juni(2018)) til 21.juni(2018) harRett true
                    fra(22.juni(2018)) harRett true
                }

                førteTil shouldBe "Gjenopptak"
            }
        }
    }

    @Test
    @Disabled("Vi kan ikke stanse direkte på harLøpendeRett")
    fun `tester automatisk stans etter oppbrukt periode`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            // Simuler siste ende på perioden
            saksbehandler.leggTilOpplysning(antallStønadsdager, 5)

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder shouldHaveSize 1
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(2) {
                rettighetsperioder {
                    fra(21.juni(2018)) til 27.juni(2018) harRett true
                    fra(28.juni(2018)) harRett false
                }
            }
        }
    }

    @Test
    fun `mangler fastsettelse av arbeidstid `() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(8.juni(2026), ønskerFraDato = 15.juni(2026))
            behovsløsere.løsTilForslag()
            behandlingsresultatForslag {
                rettighetsperioder {
                    fra(15.juni(2026)) harRett true
                }
                with(opplysninger(fastsattVanligArbeidstid)) {
                    this shouldHaveSize 1
                }
            }
            saksbehandler.leggTilOpplysning(
                nyArbeidstid,
                verdi = 37.5,
                gyldighetsperiode = Gyldighetsperiode(15.juni(2026), 21.juni(2026)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                nyArbeidstid,
                verdi = 0.0,
                gyldighetsperiode = Gyldighetsperiode(22.juni(2026)),
            )
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                rettighetsperioder {
                    fra(22.juni(2026)) harRett true
                }
                with(opplysninger(kravTilTaptArbeidstid)) {
                    this shouldHaveSize 1
                }
                with(opplysninger(kravTilArbeidstidsreduksjon)) {
                    size shouldBe 1
                }
                opplysning(fastsattVanligArbeidstid) {
                    fra(22.juni(2026)) verdi 37.5
                }
            }

            saksbehandler.leggTilOpplysning(
                beregnetArbeidstid,
                verdi = 23.5,
                gyldighetsperiode = Gyldighetsperiode(22.juni(2026)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                beregningsregel6mnd,
                verdi = false,
                gyldighetsperiode = Gyldighetsperiode(22.juni(2026)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                beregningsregel12mnd,
                verdi = true,
                gyldighetsperiode = Gyldighetsperiode(22.juni(2026)),
            )
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                rettighetsperioder {
                    fra(22.juni(2026)) harRett true
                }
                opplysning(fastsattVanligArbeidstid) {
                    fra(22.juni(2026)) verdi 23.5
                }

                with(opplysninger(kravTilArbeidstidsreduksjon)) {
                    size shouldBe 1
                }
            }
        }
    }

    @Test
    fun `revurdering hvor det skal legges inn en stans fordi bruker ikke var registrert en periode`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(15.juni(2026))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(15.juni(2026)) harRett true
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            saksbehandler.omgjørBehandling(3.juli(2026))
            saksbehandler.leggTilOpplysning(oppyllerKravTilRegistrertArbeidssøker, true, "C", Gyldighetsperiode(3.juli(2026)))
            saksbehandler.leggTilOpplysning(
                oppyllerKravTilRegistrertArbeidssøker,
                false,
                "B",
                Gyldighetsperiode(22.juni(2026), 2.juli(2026)),
            )

            behandlingsresultatForslag(5) {
                opplysninger(oppyllerKravTilRegistrertArbeidssøker) shouldHaveSize 3
                rettighetsperioder {
                    fra(15.juni(2026)) til 21.juni(2026) harRett true
                    fra(22.juni(2026)) til 2.juli(2026) harRett false
                    fra(3.juli(2026)) harRett true
                }
            }
        }
    }

    @Test
    fun `revurdering hvor det skal endres lengde å unntak for utdanning og utdanning avsluttes etter unntaket`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(19.mai(2026))
            behovsløsere.løsTilForslag()
            saksbehandler.leggTilOpplysning(kravPåLønn, true, "A", Gyldighetsperiode(19.mai(2026), 31.mai(2026)))
            behovsløsere.løsTilForslag()
            saksbehandler.leggTilOpplysning(kravPåLønn, false, "A", Gyldighetsperiode(1.juni(2026)))
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(1.juni(2026)) harRett true
                }
            }

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(2) {
                førteTil shouldBe Endring.toString()
            }

            person.sendInnMeldekort(3, aktiviteter = listOf(MeldekortAktivitet.Utdanning(timer = 0)))
            val meldekort = meldekortBatch(markerFerdig = false)

            saksbehandler.leggTilOpplysning(deltakelseIArbeidsmarkedstiltak, true, "A", Gyldighetsperiode(8.juni(2026), 3.juli(2026)))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(3) {
                rettighetsperioder {
                    fra(1.juni(2026)) til 3.juli(2026) harRett true
                    fra(4.juli(2026)) harRett false
                }

                meldekortFerdig(meldekort.last())
            }

            person.sendInnMeldekort(4)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(4) {
                førteTil shouldBe Endring.toString()
                rettighetsperioder {
                    fra(1.juni(2026)) til 3.juli(2026) harRett true
                    fra(4.juli(2026)) harRett false
                }
            }

            saksbehandler.omgjørBehandling(15.juli(2026))

            saksbehandler.leggTilOpplysning(Utdanning.tarUtdanning, false, "A", Gyldighetsperiode(4.juli(2026), 26.juli(2026)))
            saksbehandler.leggTilOpplysning(Utdanning.tarUtdanning, true, "A", Gyldighetsperiode(27.juli(2026), 1.januar(2027)))
            saksbehandler.leggTilOpplysning(Utdanning.tarUtdanning, false, "A", Gyldighetsperiode(2.januar(2027)))

            // Får godkjent den ene dagen med informasjonsmøte
            // TODO: Om man er uheldig og gjør en feil først og sletter opplysningen så kommer vi helt i usynk og alt blir feil
            // saksbehandler.endreOpplysning(deltakelseIArbeidsmarkedstiltak, true, "A", Gyldighetsperiode(2.juni(2026), 2.juli(2026)))
            // saksbehandler.fjernOpplysning(deltakelseIArbeidsmarkedstiltak)
            saksbehandler.leggTilOpplysning(deltakelseIArbeidsmarkedstiltak, true, "A", Gyldighetsperiode(2.juni(2026), 2.juni(2026)))

            // Får godkjent ny periode med arbeidsmarkedstiltak
            saksbehandler.leggTilOpplysning(deltakelseIArbeidsmarkedstiltak, true, "A", Gyldighetsperiode(27.juli(2026), 1.januar(2027)))
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(5) {
                rettighetsperioder {
                    fra(1.juni(2026)) harRett true
                }

                opplysninger(Utdanning.tarUtdanning) {
                    val (sanne, usanne) = this.partition { it.verdi.verdi == true }
                    sanne shouldHaveSize 2
                    usanne shouldHaveSize 3
                }

                opplysninger(Utdanning.deltakelseIArbeidsmarkedstiltak) {
                    val (sanne, usanne) = this.partition { it.verdi.verdi == true }
                    sanne shouldHaveSize 3
                    usanne shouldHaveSize 4
                }

                opplysninger(Utdanning.kravTilUtdanning) {
                    shouldHaveSize(8)
                    this shouldForAll { require(it.verdi.verdi == true) }
                }
            }
        }
    }

    @Test
    @Disabled("Denne må vi fikse etterpå")
    fun `ny søknad med ønsket dato i framtiden skal ikke legge seg over opphold fra forrige behandling`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            // Første søknad: opphold satt av saksbehandler fra 1. mai til 17. mai
            person.søkDagpenger(1.mai(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(
                oppholdINorge,
                true,
                "Opphold i Norge",
                Gyldighetsperiode(fraOgMed = 1.mai(2018), tilOgMed = 17.mai(2018)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                opplysning(oppholdINorge) {
                    fra(1.mai(2018)) til 17.mai(2018) verdi true
                }
            }

            // Ny søknad 4. mai med ønsket dato 31. mai (søknadsdato er også 4. mai)
            person.søkDagpenger(4.mai(2018), ønskerFraDato = 31.mai(2018))
            behovsløsere.løsTilForslag()

            // Det nye forslaget skal IKKE ha en oppholds-opplysning som overlapper med 1-17. mai
            behandlingsresultatForslag {
                opplysning(oppholdINorge) {
                    fra(1.mai(2018)) til 17.mai(2018) verdi true opprinnelse Opplysningsperiode.Periodestatus.Arvet
                    fra(18.mai(2018)) verdi true opprinnelse Opplysningsperiode.Periodestatus.Ny
                }
            }
        }
    }

    @Test
    fun `tester avslag ved for lite inntekt men med en klage som gir medhold`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.åpneAvklaringer().map { it.kode } shouldNotContain (SøknadBehandletTidligere.kode)
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett false
                }
                opplysning(Alderskrav.kravTilAlder) {
                    fra(21.juni(2018)) verdi true
                }
                opplysning(Minsteinntekt.minsteinntekt) {
                    fra(21.juni(2018)) verdi false
                }
                opplysning(ReellArbeidssøker.kravTilArbeidssøker) {
                    fra(21.juni(2018)) verdi true
                }

                opplysninger shouldHaveSize 71
            }

            // får mye mer penger
            saksbehandler.vurderSøknadPåNytt(person.sisteSøknadId!!)
            saksbehandler.åpneAvklaringer().map { it.kode } shouldContain (SøknadBehandletTidligere.kode)
            behovsløsere.løsTilForslag() shouldHaveSize 14

            behandlingsresultatForslag(3) {}

            person.inntektSiste12Mnd = 500000
            saksbehandler.leggTilOpplysning(
                Søknadstidspunkt.prøvingsdato,
                1.juli(2018),
                "Klage med medhold",
                Gyldighetsperiode(1.juli(2018)),
            )
            behovsløsere.løsTilForslag() shouldHaveSize 14

            behandlingsresultatForslag(4) {
                opplysninger(prøvingsdato) {
                    this shouldHaveSize 1
                }
                opplysninger(Minsteinntekt.inntektFraSkatt) {
                    this shouldHaveSize 1
                }
                opplysning(Minsteinntekt.minsteinntekt) {
                    fra(1.juli(2018)) verdi true
                }
                opplysning(Dagpengegrunnlag.grunnlag) {
                    fra(1.juli(2018)) verdi Beløp(517349)
                }
                rettighetsperioder {
                    fra(1.juli(2018)) harRett true
                }
            }
        }
    }
}
