package no.nav.dagpenger.scenario

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering.oppfyllerKravetTilPermittering
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.erPermittert
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test

class PermitteringTest {
    @Test
    fun `tester innvilgelse ved permittering og beregning av forbruk`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch()

            behandlingsresultat(2) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 7
                }
                with(opplysninger(PermitteringFastsetting.gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            person.sendInnMeldekort(2)
            meldekortBatch()

            behandlingsresultat(3) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 17
                }
                with(opplysninger(PermitteringFastsetting.gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 113
                }
            }
        }
    }

    @Test
    fun `tester pause av permittering og beregning av forbruk`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch()

            behandlingsresultat(2) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 7
                }
                with(opplysninger(PermitteringFastsetting.gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            // Stanser permittering og forbruker bare ordinær
            saksbehandler.lagBehandling(2.juli(2018))
            saksbehandler.endreOpplysning(
                erPermittert,
                false,
                "Permitteringen er satt på pause",
                Gyldighetsperiode(2.juli(2018)),
            )
            saksbehandler.endreOpplysning(
                oppfyllerKravetTilPermittering,
                false,
                "Permitteringen er satt på pause",
                Gyldighetsperiode(2.juli(2018)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(3) {
                with(opplysninger(oppfyllerKravetTilPermittering)) {
                    this.last().verdi.verdi shouldBe false
                }
            }

            // Sender meldekort som ikke-permittert
            person.sendInnMeldekort(2)
            meldekortBatch()

            behandlingsresultat(4) {
                with(opplysninger(oppfyllerKravetTilPermittering)) {
                    this.last().verdi.verdi shouldBe false
                }
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 7
                }
                with(opplysninger(PermitteringFastsetting.gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            // Blir permittert igjen
            saksbehandler.lagBehandling(16.juli(2018))
            saksbehandler.endreOpplysning(
                erPermittert,
                true,
                "Permitteringen er startet igjen",
                Gyldighetsperiode(16.juli(2018)),
            )
            saksbehandler.endreOpplysning(
                oppfyllerKravetTilPermittering,
                true,
                "Permitteringen er startet igjen",
                Gyldighetsperiode(16.juli(2018)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(5) {}

            person.sendInnMeldekort(3)
            meldekortBatch()

            behandlingsresultat(6) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 17
                }
                with(opplysninger(PermitteringFastsetting.gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 113
                }
            }
        }
    }

    @Test
    fun `teller riktige dager ved permittering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(18.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) { }

            person.sendInnMeldekort(
                1,
                aktiviteter =
                    listOf(
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Fravær,
                        MeldekortAktivitet.Syk,
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                    ),
            )
            meldekortBatch()

            behandlingsresultat(2) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 8
                }
                with(opplysninger(PermitteringFastsetting.gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 122
                }
            }
        }
    }
}
