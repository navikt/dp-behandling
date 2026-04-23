package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.november
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.Alderskrav.fødselsdato
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Permittering.godkjentPermitteringsårsak
import no.nav.dagpenger.regel.ReellArbeidssøker
import no.nav.dagpenger.regel.ReellArbeidssøker.kanJobbeHvorSomHelst
import no.nav.dagpenger.regel.RegistrertArbeidssøker
import no.nav.dagpenger.regel.RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker
import no.nav.dagpenger.regel.Rettighetstype
import no.nav.dagpenger.regel.Rettighetstype.erReellArbeidssøkerVurdert
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.bruktBeregningsregel
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.ordinærPeriode
import no.nav.dagpenger.regel.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.vernepliktPeriode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ScenarioTest {
    @Test
    fun `tester avslag ved for høy alder`() {
        nyttScenario {
            alder = 88
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe false
                rettighetsperioder.single().fraOgMed shouldBe 21.juni(2018)

                opplysninger(Alderskrav.kravTilAlder).single().verdi.verdi shouldBe false
                opplysninger(Minsteinntekt.minsteinntekt).shouldBeEmpty()

                opplysninger shouldHaveSize 23
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

            // En etterslenger for å verifisere at vi sender ut vedtak_fattet for avslag
            rapidInspektør.message(18)["@event_name"].asText() shouldBe "vedtak_fattet"

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe false
                rettighetsperioder.single().fraOgMed shouldBe 21.juni(2018)

                opplysninger(Alderskrav.kravTilAlder).single().verdi.verdi shouldBe true
                opplysninger(Minsteinntekt.minsteinntekt).single().verdi.verdi shouldBe false
                opplysninger(ReellArbeidssøker.kravTilArbeidssøker).single().verdi.verdi shouldBe true

                opplysninger shouldHaveSize 67
            }
        }
    }

    @Test
    fun `tester avslag ved for lite inntekt uten å vurdere reell arbeidssøker`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.endreOpplysning(erReellArbeidssøkerVurdert, false)
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe false
                rettighetsperioder.single().fraOgMed shouldBe 21.juni(2018)

                opplysninger(Alderskrav.kravTilAlder).single().verdi.verdi shouldBe true
                opplysninger(Minsteinntekt.minsteinntekt).single().verdi.verdi shouldBe false
                opplysninger(ReellArbeidssøker.kravTilArbeidssøker) shouldHaveSize 0

                opplysninger shouldHaveSize 52
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

            saksbehandler.endreOpplysning(kanJobbeHvorSomHelst, false)
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(kanJobbeHvorSomHelst, true)
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().fraOgMed shouldBe 21.juni(2018)

                opplysninger(fastsattVanligArbeidstid).single().verdi.verdi shouldBe 37.5
                opplysninger(dagsatsEtterSamordningMedBarnetillegg).single().verdi.verdi shouldBe 1259

                opplysninger(bruktBeregningsregel).single().verdi.verdi shouldBe "Inntekt etter avkortning og oppjustering siste 12 måneder"
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

            saksbehandler.endreOpplysning(
                oppyllerKravTilRegistrertArbeidssøker,
                true,
                gyldighetsperiode = Gyldighetsperiode(21.juni(2018), 27.juni(2018)),
            )
            saksbehandler.endreOpplysning(
                oppyllerKravTilRegistrertArbeidssøker,
                false,
                gyldighetsperiode = Gyldighetsperiode(28.juni(2018)),
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 28.juni(2018)

                opplysninger(fastsattVanligArbeidstid).single().verdi.verdi shouldBe 37.5
                opplysninger(dagsatsEtterSamordningMedBarnetillegg).single().verdi.verdi shouldBe 1259

                opplysninger(bruktBeregningsregel).single().verdi.verdi shouldBe "Inntekt etter avkortning og oppjustering siste 12 måneder"
            }
        }
    }

    // TODO: Lag en dedikert test som verifiserer at behandlingsresultat har ident til saksbehandler og beslutter
    // TODO: Lag en dedikert test som verifiserer at behandlingsresultat om behandlingen er automatisk

    @Test
    fun `tester innvilgelse ved permittering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
            }
        }
    }

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
                rettighetsperioder.single().harRett shouldBe true

                opplysninger(dagpengegrunnlag).single().verdi.verdi shouldBe 52490
                opplysninger(grunnlag).single().verdi.verdi shouldBe 319197
                opplysninger(fastsattVanligArbeidstid).single().verdi.verdi shouldBe 37.5
                opplysninger(dagsatsEtterSamordningMedBarnetillegg).single().verdi.verdi shouldBe 783
                opplysninger(ordinærPeriode).single().verdi.verdi shouldBe 0
                opplysninger(vernepliktPeriode).single().verdi.verdi shouldBe 26
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

            saksbehandler.endreOpplysning(
                godkjentPermitteringsårsak,
                true,
                "Båten suser",
                Gyldighetsperiode(21.juni(2018), 22.juni(2018)),
            )
            saksbehandler.endreOpplysning(
                godkjentPermitteringsårsak,
                false,
                "Båten suser",
                Gyldighetsperiode(23.juni(2018), 26.juni(2018)),
            )
            saksbehandler.endreOpplysning(
                godkjentPermitteringsårsak,
                true,
                "Båten suser",
                Gyldighetsperiode(27.juni(2018), 29.juni(2018)),
            )
            saksbehandler.endreOpplysning(
                godkjentPermitteringsårsak,
                false,
                "Båten suser",
                Gyldighetsperiode(30.juni(2018), 2.juli(2018)),
            )
            saksbehandler.endreOpplysning(
                godkjentPermitteringsårsak,
                true,
                "Båten suser",
                Gyldighetsperiode(3.juli(2018), 3.juli(2018)),
            )
            saksbehandler.endreOpplysning(
                godkjentPermitteringsårsak,
                false,
                "Båten suser",
                Gyldighetsperiode(4.juli(2018), 5.juli(2018)),
            )
            saksbehandler.endreOpplysning(
                godkjentPermitteringsårsak,
                true,
                "Båten suser",
                Gyldighetsperiode(6.juli(2018), 6.juli(2018)),
            )
            saksbehandler.endreOpplysning(godkjentPermitteringsårsak, false, "Båten suser", Gyldighetsperiode(7.juli(2018)))

            behandlingsresultatForslag {
                rettighetsperioder shouldHaveSize 8
            }
        }
    }

    @Test
    @Disabled("Skrus av for å teste innvilgelse med mange perioder")
    fun `innvilgelse der en er permittering fra fiskeforedling OG tillegg søker om dagpenger etter verneplikt`() {
        nyttScenario {
            inntektSiste12Mnd = 10
            permittertfraFiskeforedling = true
            verneplikt = true
        }.test {
            person.søkDagpenger(21.juni(2018), ønskerFraDato = 22.juni(2018))

            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(1) {
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 22.juni(2018)
                with(opplysninger(Rettighetstype.permitteringFiskeforedling)) {
                    size shouldBe 1
                    first().verdi.verdi shouldBe true
                }
                with(opplysninger(Dagpengeperiode.ordinærPeriode)) {
                    size shouldBe 1
                    first().verdi.verdi shouldBe 0
                }
                with(opplysninger(VernepliktFastsetting.vernepliktPeriode)) {
                    size shouldBe 1
                    first().verdi.verdi shouldBe 26
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
    @Disabled("Ikke mulig å sette prøvingsdato direkte, ønsker dato må manipuleres")
    fun `tester avslag ved for lite inntekt, ikke reell arbeidssøker, og prøvingsdato flyttes til søknadsdato`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2018), 25.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.endreOpplysning(erReellArbeidssøkerVurdert, false, "Kan ikke vurdere reell arbeidssøker")
            saksbehandler.endreOpplysning(
                prøvingsdato,
                21.juni(2018),
                "Avslag skal være fra søknadsdato",
                Gyldighetsperiode(21.juni(2018)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe false
                rettighetsperioder.single().fraOgMed shouldBe 21.juni(2018)

                opplysninger(Alderskrav.kravTilAlder).single().verdi.verdi shouldBe true
                opplysninger(Minsteinntekt.minsteinntekt).single().verdi.verdi shouldBe false

                opplysninger shouldHaveSize 43
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
            saksbehandler.endreOpplysning(
                oppfyllerKravetTilVerneplikt,
                false,
                "False for alltid",
                Gyldighetsperiode(),
            )
            saksbehandler.endreOpplysning(
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

            saksbehandler.endreOpplysning(
                søknadIdOpplysningstype,
                person.sisteSøknadId!!.toString(),
                "Søkte for lenge siden",
                Gyldighetsperiode(1.juni(2018)),
            )

            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(2) {
                with(rettighetsperioder) {
                    this shouldHaveSize 2
                    this[0].fraOgMed shouldBe 1.juni(2018)
                    this[0].harRett shouldBe false
                    this[1].fraOgMed shouldBe 27.november(2018)
                    this[1].harRett shouldBe true
                }
            }

            saksbehandler.endreOpplysning(
                RegistrertArbeidssøker.registrertArbeidssøker,
                true,
                "Søkte for lenge siden",
                Gyldighetsperiode(1.juni(2018)),
            )
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(3) {
                with(rettighetsperioder) {
                    this shouldHaveSize 1
                    single().fraOgMed shouldBe 1.juni(2018)
                    single().tilOgMed shouldBe null
                    single().harRett shouldBe true
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
                opplysninger(prøvingsdato).single().verdi.verdi shouldBe 21.juni(2021).toString()
            }

            saksbehandler.endreOpplysning(
                ReellArbeidssøker.kravTilArbeidssøker,
                true,
                "Flyttet prøvingsdato automatisk",
                Gyldighetsperiode(25.juni(2021)),
            )

            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                // opplysninger(prøvingsdato).single().verdi.verdi shouldBe 25.juni(2021).toString()
            }

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()
        }
    }

    @Test
    @Disabled("Arbeidssøkerregistrering må svare med riktig data")
    fun `prøver ikke datoer før søknadstidspunkt`() {
        nyttScenario {
            // registreringsdato = 1.januar(2021)
        }.test {
            person.søkDagpenger(21.juni(2021))

            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                opplysninger(prøvingsdato).single().verdi.verdi shouldBe 21.juni(2021).toString()
            }
        }
    }
}
