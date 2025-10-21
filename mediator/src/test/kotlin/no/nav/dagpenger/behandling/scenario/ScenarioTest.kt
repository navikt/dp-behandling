package no.nav.dagpenger.behandling.scenario

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.OpplysningsverdiDTO
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import no.nav.dagpenger.behandling.api.models.UlidVerdiDTO
import no.nav.dagpenger.behandling.august
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.helpers.scenario.assertions.Opplysningsperiode.Periodestatus
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.behandling.scenario.ScenarioTest.Formatter.lagBrev
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.Alderskrav.fødselsdato
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.Opphold.oppholdINorge
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

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

            vedtak {
                utfall shouldBe false
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

            vedtak {
                utfall shouldBe false
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

            vedtak {
                utfall shouldBe true
            }
        }
    }

    @Test
    fun `tester innvilgelse ved permittering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            forslag {
                utfall shouldBe true

                medFastsettelser {
                    periode("Permitteringsperiode") shouldBe 26
                }
            }
        }
    }

    @Test
    fun `tester innvilgelse, stans, og gjenopptak `() {
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
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                opplysninger(Opphold.oppholdINorge) shouldHaveSize 1

                lagBrev(klump.toString()).also { println(it) }
            }

            // Opprett stans
            person.opprettBehandling(22.juli(2018))
            saksbehandler.endreOpplysning(oppholdINorge, false, "Er i utlandet", Gyldighetsperiode(22.juli(2018)))
            saksbehandler.endreOpplysning(harLøpendeRett, false, "Har ikke krav", Gyldighetsperiode(22.juli(2018)))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 22.juli(2018)

                lagBrev(klump.toString()).also {
                    println(it)
                }

                with(opplysninger(Opphold.oppholdINorge)) {
                    this shouldHaveSize 2
                    this[0].status shouldBe Periodestatus.Arvet
                    this[1].status shouldBe Periodestatus.Ny
                }
                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                }
            }

            // Gjenoppta
            person.opprettBehandling(23.august(2018))
            saksbehandler.endreOpplysning(oppholdINorge, true, "Tilbake fra utlandet", Gyldighetsperiode(23.august(2018)))
            saksbehandler.endreOpplysning(harLøpendeRett, true, "Har krav", Gyldighetsperiode(23.august(2018)))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                    this[2].verdi.verdi shouldBe true
                }

                lagBrev(klump.toString()).also { println(it) }

                rettighetsperioder shouldHaveSize 3
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 22.juli(2018)

                rettighetsperioder[2].harRett shouldBe true
                rettighetsperioder[2].fraOgMed shouldBe 23.august(2018)

                with(opplysninger(Opphold.oppholdINorge)) {
                    this shouldHaveSize 3
                    this[0].status shouldBe Periodestatus.Arvet
                    this[1].status shouldBe Periodestatus.Arvet
                    this[2].status shouldBe Periodestatus.Ny
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
            person.behandling.harOpplysning(opplysning.id) shouldBe false
        }
    }

    object Formatter {
        private val outFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        fun d(date: java.time.LocalDate?): String? = date?.format(outFmt)

        fun periodetekst(
            fra: java.time.LocalDate?,
            til: java.time.LocalDate?,
        ): String =
            when {
                fra != null && til != null -> "${d(fra)} – ${d(til)}"
                fra != null -> "fra ${d(fra)}"
                til != null -> "til ${d(til)}"
                else -> "(uten angitt periode)"
            }

        fun verdiSomTekst(verdi: OpplysningsverdiDTO): String {
            return when (verdi) {
                is BarnelisteDTO ->
                    verdi.verdi.forEach { verdi ->
                        val fødselsdato = d(verdi.fødselsdato)
                        val navn = verdi.fornavnOgMellomnavn ?: ("Ukjent navn" + (verdi.etternavn?.let { " $it" } ?: ""))
                        return "Barn: $navn, født $fødselsdato"
                    }

                is BoolskVerdiDTO ->
                    when (verdi.verdi) {
                        true -> "Ja"
                        false -> "Nei"
                    }

                is DatoVerdiDTO -> d(verdi.verdi) ?: "-"
                is DesimaltallVerdiDTO -> verdi.verdi.toString()
                is HeltallVerdiDTO -> verdi.verdi.toString()
                is PengeVerdiDTO -> verdi.verdi.toString() + " kr"
                is PeriodeVerdiDTO -> periodetekst(verdi.fom, verdi.tom)
                is TekstVerdiDTO -> verdi.verdi
                is UlidVerdiDTO -> verdi.verdi
            } as String
        }

        fun lagBrev(json: String): String {
            val data = objectMapper.readValue<BehandlingsresultatDTO>(json)

            val dato = java.time.LocalDate.now()
            val nyeOpplysninger =
                data.opplysninger
                    .mapNotNull { opp ->
                        val nyePerioder = opp.perioder?.filter { it.status == OpprinnelseDTO.NY }
                        if (nyePerioder?.isEmpty() == true) {
                            null
                        } else {
                            opp.navn to nyePerioder
                        }
                    }

            val sb = StringBuilder()
            sb.appendLine("Vedtak – Dagpenger")
            sb.appendLine("Behandling: ${data.behandlingId}")
            sb.appendLine("Dato: $dato")
            sb.appendLine().appendLine("Hei,")
            sb.appendLine("Vi har behandlet saken din. Nedenfor oppsummerer vi nye opplysninger og perioder med/uten rett til dagpenger.")
            sb.appendLine()

            sb.appendLine("Nye opplysninger:")
            if (nyeOpplysninger.isEmpty()) {
                sb.appendLine("– Ingen nye opplysninger.")
            } else {
                nyeOpplysninger.forEach { (navn, perioder) ->
                    perioder?.forEach { periode ->
                        val periodeTxt =
                            Formatter.periodetekst(
                                periode.gyldigFraOgMed,
                                periode.gyldigTilOgMed,
                            )
                        val verdiTxt = Formatter.verdiSomTekst(periode.verdi)
                        sb.appendLine("– $navn: $verdiTxt ($periodeTxt)")
                    }
                }
            }
            sb.appendLine()

            sb.appendLine("Rettighetsperioder:")
            if (data.rettighetsperioder.isEmpty()) {
                sb.appendLine("– Ingen registrerte rettighetsperioder.")
            } else {
                data.rettighetsperioder.forEach { rettighetsperiodeDTO ->
                    val periodeTxt =
                        Formatter.periodetekst(
                            rettighetsperiodeDTO.fraOgMed,
                            rettighetsperiodeDTO.tilOgMed,
                        )
                    val rettTxt = if (rettighetsperiodeDTO.harRett) "har rett" else "har ikke rett"
                    sb.appendLine("– $periodeTxt: $rettTxt til dagpenger.")
                }
            }

            return sb.toString().trimEnd()
        }
    }
}
