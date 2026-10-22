package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.DagpengerRegistrering
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate

class MigrerYtelseMottakTest {
    private val ekstra = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "ekstra")
    private val rapid = TestRapid()
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)

    init {
        MigrerYtelseMottak(
            rapidsConnection = rapid,
            hendelseMottaker = hendelseMottaker,
            opplysningstyper = DagpengerRegistrering().opplysningstyper,
        )
    }

    @Test
    fun `oppretter behandling med kontraktsfelter og tilleggsopplysninger`() {
        rapid.sendTestMessage(ekteMelding())

        val hendelse = slot<MigrerYtelseHendelse>()
        verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }

        val behandling =
            hendelse.captured
                .behandling(null, TemporalCollection())
                .shouldBeInstanceOf<StartHendelseResultat.Opprettet>()
                .behandling
        val opplysninger = behandling.opplysninger()

//        opplysninger.finnOpplysning(ekstra).verdi shouldBe true
        opplysninger.finnOpplysning(DagpengenesStørrelse.antallBarn).verdi shouldBe 0
        opplysninger.finnOpplysning(Dagpengeperiode.antallStønadsdager).verdi shouldBe 520
        opplysninger.finnOpplysning(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg).verdi shouldBe Beløp(1500)
        opplysninger.finnOpplysning(Dagpengegrunnlag.grunnlag).verdi shouldBe Beløp(310630)
    }

    @Disabled
    @Test
    fun `oppretter faktum fra opplysningsliste i start_migrering`() {
        val gyldigFraOgMed = LocalDate.of(2025, 11, 17)
        val opplysning =
            """
            [{
              "navn": "visningsnavn",
              "verdi": true,
              "gyldigFraOgMed": "$gyldigFraOgMed",
              "uuid": "${ekstra.id.uuid}"
            }]
            """.trimIndent()

        rapid.sendTestMessage(melding(opplysninger = opplysning, eventName = "start_migrering"))

        val hendelse = slot<MigrerYtelseHendelse>()
        verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }

        val behandling =
            hendelse.captured
                .behandling(null, TemporalCollection())
                .shouldBeInstanceOf<StartHendelseResultat.Opprettet>()
                .behandling
        val faktum = behandling.opplysninger().finnOpplysning(ekstra)

        faktum.verdi shouldBe true
        faktum.gyldighetsperiode shouldBe Gyldighetsperiode(gyldigFraOgMed)
    }

    @Test
    fun `leser opplysningsliste fra nytt format`() {
        val melding = JsonMapper.builder().build().readTree(nyttFormat())
        val opplysninger = melding["opplysninger"].tilleggsopplysninger(DagpengerRegistrering().opplysningstyper)

        opplysninger.size shouldBe 15
        opplysninger.first().verdi shouldBe true
        opplysninger.first().gyldighetsperiode shouldBe Gyldighetsperiode(LocalDate.of(2025, 11, 17))
    }

    @Test
    fun `kan ikke oppgi samme opplysning både eksplisitt og som tilleggsopplysning`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(melding(opplysninger = """{ "${DagpengenesStørrelse.antallBarn.behovId}": 3 }"""))
        }
    }

    @Test
    fun `avviser tilleggsopplysning med feil verdityper`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(melding(opplysninger = """{ "${ekstra.behovId}": "ja" }"""))
        }
    }

    @Test
    fun `avviser ukjent opplysningstype`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(melding(opplysninger = """{ "finnes ikke": true }"""))
        }
    }

    @Disabled
    @Test
    fun `avviser opplysningsliste med ukjent uuid`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(
                melding(
                    opplysninger =
                        """[{"navn":"ukjent","verdi":true,"gyldigFraOgMed":"2025-11-17","uuid":"0194881f-9443-72b4-8b30-5f6cdb24d54c"}]""",
                    eventName = "start_migrering",
                ),
            )
        }
    }

    private fun melding(
        opplysninger: String,
        eventName: String = "migrer_ytelse",
    ) = """
        {
          "@event_name": "$eventName",
          "ident": "12345678910",
          "innvilgetFraOgMed": "${LocalDate.of(2026, 1, 5)}",
          "innvilgedeStønadsuker": 52,
          "gjenståendeStønadsdager": 120,
          "dagsats": "800",
          "antallBarn": 2,
          "vanligArbeidstidPerUke": 37.5,
          "grunnlag": "517349",
          "opplysninger": $opplysninger
        }
        """.trimIndent()

    private fun ekteMelding() =
        """
        {
          "@event_name": "start_migrering",
          "sakId": 13699878,
          "initiertAv": "Z990798",
          "opplysninger": [
            {
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9443-72b4-8b30-5f6cdb24d54c"
            },
            {
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9443-72b4-8b30-5f6cdb24d54e"
            },
            {
              "verdi": 310630,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9410-7481-b263-4606fdd10cbd"
            },
            {
              "verdi": "2024-08-31",
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9414-7823-8d29-0e25b7feb7d0"
            },
            {
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9442-707b-a6ee-e96c06877bd8"
            },
            {
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9442-707b-a6ee-e96c06877bdb"
            },
            {
              "verdi": 3730,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9428-74d5-b160-f63a4c61a24e"
            },
            {
              "verdi": 3730,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9428-74d5-b160-f63a4c61a240"
            },
            {
              "verdi": false,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9428-74d5-b160-f63a4c61a250"
            },
            {
              "verdi": 120000,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9410-7481-b263-4606fdd10cad"
            },
            {
              "verdi": 60000,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9410-7481-b263-4606fdd10cae"
            },
            {
              "verdi": 14000000,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9410-7481-b263-4606fdd10caf"
            },
            {
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-940b-76ff-acf5-ba7bcb367237"
            },
            {
              "verdi": 746,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-942f-7bde-ab16-68ffd19e9a33"
            },
            {
              "verdi": false,
              "gyldigFraOgMed": "2025-01-31",
              "uuid": "0194881f-9442-707b-a6ee-e96c06877be1"
            }
          ],
          "ident": "26867799140",
          "@id": "1291622c-04d5-44fe-8cef-2a9da8d9e635",
          "@opprettet": "2026-10-22T12:24:12.317783439",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "1291622c-04d5-44fe-8cef-2a9da8d9e635",
              "time": "2026-10-22T12:24:12.317783439",
              "service": "dp-migrering",
              "instance": "dp-migrering-d97585f6f-5s4ns",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-migrering:2026.10.22-10.20-e675bc2@sha256:e1a3e8d199374c2379903ed5ed232a02efa316ba701b6a2057683cd511f5ca90"
            },
            {
              "id": "1291622c-04d5-44fe-8cef-2a9da8d9e635",
              "time": "2026-10-22T12:24:12.321039974",
              "service": "dp-behandling",
              "instance": "dp-behandling-6b5f464587-dsc8x",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2026.10.22-09.36-5aa0257@sha256:dae4fdceabb3ba11b7e0508685467d89f0997de6147a552160518349fd4d10d7"
            }
          ]
        }
        """.trimIndent()

    private fun nyttFormat() =
        """
        {
          "@event_name" : "start_migrering",
          "sakId" : 15603478,
          "ident" : 12345678901,
          "initiertAv" : "user1",
          "opplysninger" : [ {
            "navn" : "Bruker er medlem av folketrygden",
            "verdi" : true,
            "gyldigFraOgMed" : "2025-11-17",
            "uuid" : "0194881f-9443-72b4-8b30-5f6cdb24d54c"
          }, {
            "navn" : "Bruker har opphold i Norge",
            "verdi" : true,
            "gyldigFraOgMed" : "2025-11-17",
            "uuid" : "0194881f-9443-72b4-8b30-5f6cdb24d54e"
          }, {
            "navn" : "Dagpengegrunnlag for bruker",
            "verdi" : 267606,
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-9410-7481-b263-4606fdd10cbd"
          }, {
            "navn" : "SisteAvsluttendeKalendrmåned for bruker",
            "verdi" : "2025-10-31",
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-9414-7823-8d29-0e25b7feb7d0"
          }, {
            "navn" : "Bruker oppfyller kravet til heltid- og deltidsarbeid",
            "verdi" : true,
            "gyldigFraOgMed" : "2026-01-16",
            "uuid" : "0194881f-9442-707b-a6ee-e96c06877bd8"
          }, {
            "navn" : "Bruker oppfyller kravet til mobilitet",
            "verdi" : true,
            "gyldigFraOgMed" : "2026-01-16",
            "uuid" : "0194881f-9442-707b-a6ee-e96c06877bdb"
          }, {
            "navn" : "Ukessats med barnetillegg etter samordning",
            "verdi" : 3590,
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-9428-74d5-b160-f63a4c61a24e"
          }, {
            "navn" : "Ukessats med barnetillegg før samordning",
            "verdi" : 3590,
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-9428-74d5-b160-f63a4c61a240"
          }, {
            "navn" : "Er samordning utført",
            "verdi" : false,
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-9428-74d5-b160-f63a4c61a250"
          }, {
            "navn" : "Utbetalt inntekt periode 1",
            "verdi" : 260152,
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-9410-7481-b263-4606fdd10cad"
          }, {
            "navn" : "Utbetalt inntekt periode 2",
            "verdi" : 237376,
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-9410-7481-b263-4606fdd10cad"
          }, {
            "navn" : "Utbetalt inntekt periode 3",
            "verdi" : 91980,
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-9410-7481-b263-4606fdd10cad"
          }, {
            "navn" : "Bruker oppfyller kravet til alder",
            "verdi" : true,
            "gyldigFraOgMed" : "2025-11-17",
            "uuid" : "0194881f-940b-76ff-acf5-ba7bcb367237"
          }, {
            "navn" : "Dagsats uten barnetillegg samordnet for bruker",
            "verdi" : 642,
            "gyldigFraOgMed" : "2026-01-15",
            "uuid" : "0194881f-942f-7bde-ab16-68ffd19e9a33"
          }, {
            "navn" : "Oppfyller kravet til å være registrert som arbeidssøker",
            "verdi" : true,
            "gyldigFraOgMed" : "2026-01-16",
            "uuid" : "0194881f-9442-707b-a6ee-e96c06877be1"
          } ],
          "@id" : "3b61218a-1d07-4ca4-b3a0-874dc7caafe2",
          "@opprettet" : "2026-10-22T09:23:57.53587",
          "system_read_count" : 0,
          "system_participating_services" : [ {
            "id" : "3b61218a-1d07-4ca4-b3a0-874dc7caafe2",
            "time" : "2026-10-22T09:23:57.535870"
          } ]
        }
        """.trimIndent()
}
