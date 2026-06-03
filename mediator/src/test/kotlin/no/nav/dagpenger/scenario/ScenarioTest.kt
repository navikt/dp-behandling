package no.nav.dagpenger.scenario

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.mediator.mai
import no.nav.dagpenger.mediator.mars
import no.nav.dagpenger.mediator.november
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.bruktBeregningsregel
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.antallBarn
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.ordinærPeriode
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.regelsett.fastsetting.VernepliktFastsetting
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
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.kravetReellArbeidsøkerSkalVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.ønsketdato
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            rapidInspektør.message(16)["@event_name"].asString() shouldBe "vedtak_fattet"

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
            kanJobbeDeltid = false
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            // Avklaring om reell arbeidssøker dukker opp
            with(saksbehandler.åpneAvklaringer()) {
                map { it.kode } shouldContain "ReellArbeidssøkerUnntak"
                this shouldHaveSize 7
            }

            saksbehandler.endreOpplysning(kravetReellArbeidsøkerSkalVurderes, false)
            with(saksbehandler.lukkAlleAvklaringer()) {
                // Avklaringen for reell arbeidssøker forsvinner automatisk om det ikke skal vurderes
                map { it.kode } shouldNotContain "ReellArbeidssøkerUnntak"
                this shouldHaveAtMostSize 6
            }
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
    fun `tester innvilgelse med forskjøvet innvilgelse fram i tid`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                opplysninger(Opptjeningstid.sisteAvsluttendendeKalenderMåned).single().verdi.verdi shouldBe 31.mai(2018).toString()
            }
            saksbehandler.endreOpplysning(
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
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().fraOgMed shouldBe 7.juli(2018)

                opplysninger(Opptjeningstid.sisteAvsluttendendeKalenderMåned).single().verdi.verdi shouldBe 30.juni(2018).toString()
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
    fun `tester avslag ved for lite inntekt, ikke reell arbeidssøker, med avslag fra ønsket dato`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2018), 25.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.endreOpplysning(kravetReellArbeidsøkerSkalVurderes, false, "Kan ikke vurdere reell arbeidssøker")
            saksbehandler.endreOpplysning(
                prøvingsdato,
                25.juni(2018),
                "Avslag skal være fra ønsøknadsdato",
                Gyldighetsperiode(25.juni(2017)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe false
                rettighetsperioder.single().fraOgMed shouldBe 25.juni(2018)

                opplysninger(Alderskrav.kravTilAlder).single().verdi.verdi shouldBe true
                opplysninger(Minsteinntekt.minsteinntekt).single().verdi.verdi shouldBe false
                opplysninger(ReellArbeidssøker.kravTilArbeidssøker) shouldHaveSize 0

                opplysninger shouldHaveSize 52
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

            saksbehandler.endreOpplysning(
                ønsketdato,
                1.juni(2018),
                "Søkte for lenge siden",
                Gyldighetsperiode(1.juni(2018)),
            )
            behovsløsere.løsTilForslag()

            // Tester at vi ikke kan legge til opplysninger som ikke er dekket innenfor perioden  du har rett på dagpenger for uten at vilkårene for perioden er oppfylt
            assertThrows<IllegalArgumentException> {
                saksbehandler.endreOpplysning(
                    oppyllerKravTilRegistrertArbeidssøker,
                    true,
                    "Søkte for lenge siden",
                    Gyldighetsperiode(1.juni(2018)),
                )
            }

            behandlingsresultatForslag(3) {
                with(rettighetsperioder) {
                    this shouldHaveSize 1
                    this[0].fraOgMed shouldBe 27.november(2018)
                    this[0].harRett shouldBe true
                }
            }
        }
    }

    @Test
    fun `sjekk at vi ikke går i loop når det kommer opplysninger som ikke har avhengighetene sine i samme gyldighetsperiode`() {
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

            // Tester at vi ikke kan legge til opplysninger som ikke er dekket innenfor perioden  du har rett på dagpenger for uten at vilkårene for perioden er oppfylt
            assertThrows<IllegalArgumentException> {
                saksbehandler.endreOpplysning(
                    antallBarn,
                    22,
                    "Fikk en haug med barn",
                    Gyldighetsperiode(5.juni(2018)),
                )
            }

            behandlingsresultatForslag(2) {
                with(rettighetsperioder) {
                    this shouldHaveSize 1
                    this[0].fraOgMed shouldBe 27.november(2018)
                    this[0].harRett shouldBe true
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
                opplysninger(prøvingsdato).single().verdi.verdi shouldBe 25.juni(2021).toString()
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
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe true
            }

            // Meldinger om mulig samordning blir opprettet som en manuell behandling med avklaring
            person.fåAnnenYtelse(24.juni(2021), "SYK")
            behandlingsresultatForslag(3) {
                behandlingId shouldNotBe innvilgelseId
                rettighetsperioder shouldHaveSize 1
            }

            with(saksbehandler.åpneAvklaringer().single()) {
                kode shouldBe "ManuellBehandling"
                beskrivelse shouldContain "SYK"
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
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe false
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

            saksbehandler.endreOpplysning(oppholdINorge, true, gyldighetsperiode = Gyldighetsperiode(1.juni(2018), 10.juni(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(oppholdINorge, true, gyldighetsperiode = Gyldighetsperiode(20.juni(2018), 30.juni(2018)))
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(3) {
                opplysninger(oppholdINorge) shouldHaveSize 2
            }

            saksbehandler.endreOpplysning(kanJobbeHvorSomHelst, true, gyldighetsperiode = Gyldighetsperiode(1.juni(2018), 8.juni(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(kanJobbeHvorSomHelst, true, gyldighetsperiode = Gyldighetsperiode(11.juni(2018), 12.juni(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(kanJobbeHvorSomHelst, true, gyldighetsperiode = Gyldighetsperiode(15.juni(2018)))
            behovsløsere.løsTilForslag()

            saksbehandler.fjernOpplysning(kanJobbeHvorSomHelst)
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(7) {
                // Det er nå en ekstra periode mellom 10 og 20. juni som ikke var nødvendig før det ble lagt til instanser av kanJobbeHvorSomHelst i hullet
                opplysninger(oppholdINorge) shouldHaveSize 3

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
                true,
                "Endres",
                Gyldighetsperiode(fraOgMed = 10.juni(2018)),
            )
            behovsløsere.løsTilForslag() shouldHaveSize 8

            behandlingsresultatForslag(2) {
                with(opplysninger(oppholdINorge)) {
                    this shouldHaveSize 1
                    this[0].gyldigFraOgMed shouldNotBe 1.juni(2018)
                    this[0].gyldigFraOgMed shouldBe 10.juni(2018)
                    this[0].gyldigTilOgMed shouldBe null
                }
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder[0].fraOgMed shouldBe 10.juni(2018)
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

            saksbehandler.endreOpplysning(
                oppholdINorge,
                false,
                "Endres",
                Gyldighetsperiode(fraOgMed = 1.juni(2018), tilOgMed = 9.juni(2018)),
            )
            behovsløsere.løsTilForslag() shouldHaveSize 0

            saksbehandler.endreOpplysning(
                oppholdINorge,
                true,
                "Endres",
                Gyldighetsperiode(fraOgMed = 10.juni(2018)),
            )
            behovsløsere.løsTilForslag() shouldHaveSize 8

            behandlingsresultatForslag(3) {
                with(opplysninger(oppholdINorge)) {
                    this shouldHaveSize 2
                    this[0].gyldigFraOgMed shouldBe 1.juni(2018)
                    this[0].gyldigTilOgMed shouldBe 9.juni(2018)
                    this[1].gyldigFraOgMed shouldBe 10.juni(2018)
                    this[1].gyldigTilOgMed shouldBe null
                }
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder[0].fraOgMed shouldBe 10.juni(2018)
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
            saksbehandler.endreOpplysning(
                prøvingsdato,
                flyttePrøvingsdato,
                gyldighetsperiode = Gyldighetsperiode(fraOgMed = flyttePrøvingsdato),
            )
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                rettighetsperioder.size shouldBe 1
                rettighetsperioder.first().fraOgMed shouldBe 15.mai(2026)
            }
        }
    }
}
