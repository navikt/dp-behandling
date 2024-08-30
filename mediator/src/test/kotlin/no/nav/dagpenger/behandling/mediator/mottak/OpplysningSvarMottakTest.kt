package no.nav.dagpenger.behandling.mediator.mottak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behandling.TestOpplysningstyper.boolsk
import no.nav.dagpenger.behandling.TestOpplysningstyper.inntektA
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.uuid.UUIDv7
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpplysningSvarMottakTest {
    private val rapid = TestRapid()
    private val messageMediator = mockk<MessageMediator>(relaxed = true)

    init {
        OpplysningSvarMottak(rapid, messageMediator, setOf(boolsk, inntektA))
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(messageMediator)
    }

    @Test
    fun `tillater svar for opplysning uten metadata`() {
        rapid.sendTestMessage(løsningUtenMetadata.toJson())
        val hendelse = slot<OpplysningSvarHendelse>()
        verify {
            messageMediator.behandle(capture(hendelse), any(), any())
        }

        hendelse.isCaptured shouldBe true
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.opplysninger shouldHaveSize 1
        hendelse.captured.opplysninger
            .first()
            .verdi shouldBe true
        hendelse.captured.opplysninger
            .first()
            .opplysning()
            .verdi shouldBe true
        hendelse.captured.opplysninger
            .first()
            .opplysning()
            .gyldighetsperiode shouldBe Gyldighetsperiode()
    }

    @Test
    fun `kan parse løsninger med full inntekt`() {
        rapid.sendTestMessage(løsningPåInntekt)
        val hendelse = slot<OpplysningSvarHendelse>()
        verify {
            messageMediator.behandle(capture(hendelse), any(), any())
        }

        hendelse.isCaptured shouldBe true
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.opplysninger shouldHaveSize 1
    }

    @Test
    fun `tillater svar med opplysning med metadata med fom og tom`() {
        rapid.sendTestMessage(løsningMedMetadata(gyldigFraOgMed, gyldigTilOgMed).toJson())
        val hendelse = slot<OpplysningSvarHendelse>()
        verify {
            messageMediator.behandle(capture(hendelse), any(), any())
        }

        hendelse.isCaptured shouldBe true
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.opplysninger shouldHaveSize 1
        hendelse.captured.opplysninger
            .first()
            .verdi shouldBe true
        hendelse.captured.opplysninger
            .first()
            .opplysning()
            .verdi shouldBe true
        hendelse.captured.opplysninger
            .first()
            .opplysning()
            .gyldighetsperiode shouldBe
            Gyldighetsperiode(gyldigFraOgMed, gyldigTilOgMed)
    }

    @Test
    fun `Kan ikke besvare opplysning en ikke kjenner til`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(
                løsningMedMetadata(gyldigFraOgMed, gyldigTilOgMed, "ukjentOpplysning").toJson(),
            )
        }
    }

    @Test
    fun `tillater svar med opplysning med metadata med og uten tom`() {
        rapid.sendTestMessage(løsningMedMetadata(gyldigFraOgMed, null).toJson())
        val hendelse = slot<OpplysningSvarHendelse>()
        verify {
            messageMediator.behandle(capture(hendelse), any(), any())
        }

        hendelse.captured.opplysninger
            .first()
            .opplysning()
            .gyldighetsperiode shouldBe
            Gyldighetsperiode(gyldigFraOgMed, LocalDate.MAX)
    }

    @Test
    fun `tillater svar med opplysning med metadata med og uten fom`() {
        rapid.sendTestMessage(løsningMedMetadata(null, gyldigFraOgMed).toJson())
        val hendelse = slot<OpplysningSvarHendelse>()
        verify {
            messageMediator.behandle(capture(hendelse), any(), any())
        }

        hendelse.captured.opplysninger
            .first()
            .opplysning()
            .gyldighetsperiode shouldBe
            Gyldighetsperiode(LocalDate.MIN, gyldigFraOgMed)
    }

    private val behandlingId = UUIDv7.ny()

    private val konvolutt =
        mapOf(
            "ident" to "12345678901",
            "behandlingId" to behandlingId,
            "@final" to true,
            "@opplysningsbehov" to true,
            "@behovId" to behandlingId,
        )
    private val løsningUtenMetadata =
        JsonMessage.newNeed(
            listOf("boolsk"),
            konvolutt +
                mapOf(
                    "@løsning" to
                        mapOf(
                            "boolsk" to true,
                        ),
                ),
        )
    private val gyldigFraOgMed = LocalDate.now()
    private val gyldigTilOgMed = gyldigFraOgMed.plusDays(5)

    private fun løsningMedMetadata(
        gyldigFraOgMed: LocalDate?,
        gyldigTilOgMed: LocalDate?,
        opplysningstype: String = "boolsk",
    ): JsonMessage =
        JsonMessage.newNeed(
            listOf(opplysningstype),
            konvolutt +
                mapOf(
                    "@løsning" to
                        mapOf(
                            opplysningstype to
                                mapOf(
                                    "verdi" to true,
                                ) +
                                mapOf(
                                    "gyldigFraOgMed" to gyldigFraOgMed?.toString(),
                                    "gyldigTilOgMed" to gyldigTilOgMed?.toString(),
                                ).filterValues { it != null },
                        ),
                ),
        )

    @Language("JSON")
    private val løsningPåInntekt =
        """
        {
          "@event_name": "behov",
          "@behovId": "ABBADABBADOO",
          "@behov": [
            "Inntekt"
          ],
          "ident": "12345678901",
          "behandlingId": "$behandlingId",
          "gjelderDato": "2024-08-26",
          "søknadId": "ABBADABBADOO",
          "søknad_uuid": "ABBADABBADOO",
          "opprettet": "2024-08-26T13:19:05.183155",
          "Inntekt": {
            "@opplysningsbehov": true,
            "InntektId": "ABBADABBADOO",
            "InnsendtSøknadsId": {
              "urn": "urn:soknad:ABBADABBADOO"
            },
            "søknad_uuid": "ABBADABBADOO"
          },
          "@opplysningsbehov": true,
          "InntektId": "ABBADABBADOO",
          "InnsendtSøknadsId": {
            "urn": "urn:soknad:ABBADABBADOO"
          },
          "@final": true,
          "@id": "ed48feff-ff1d-42a2-9972-5c3b0d95f04c",
          "@opprettet": "2024-08-26T13:38:41.001093444",
          "system_read_count": 2,
          "system_participating_services": [
            {
              "id": "5ee12cde-a683-4af1-8e47-08fa1d5c1217",
              "time": "2024-08-26T13:38:38.342267021",
              "service": "dp-behandling",
              "instance": "dp-behandling-5dcf7cb4dc-zxnpd",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2024.08.26-11.36-699d7af"
            },
            {
              "id": "5ee12cde-a683-4af1-8e47-08fa1d5c1217",
              "time": "2024-08-26T13:38:40.941990530",
              "service": "dp-oppslag-inntekt",
              "instance": "dp-oppslag-inntekt-9d6cd69fb-wfbpg",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-oppslag-inntekt:2024.08.21-07.07-a168d45"
            },
            {
              "id": "ed48feff-ff1d-42a2-9972-5c3b0d95f04c",
              "time": "2024-08-26T13:38:41.001093444",
              "service": "dp-oppslag-inntekt",
              "instance": "dp-oppslag-inntekt-9d6cd69fb-wfbpg",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-oppslag-inntekt:2024.08.21-07.07-a168d45"
            },
            {
              "id": "ed48feff-ff1d-42a2-9972-5c3b0d95f04c",
              "time": "2024-08-26T13:41:32.447626972",
              "service": "dp-behandling",
              "instance": "dp-behandling-5dcf7cb4dc-2bwsm",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2024.08.26-11.36-699d7af"
            }
          ],
          "@løsning": {
            "Inntekt": {
              "verdi": {
                "inntektsId": "01J677GHJRC2H08Q55DASFD0XX",
                "inntektsListe": [
                  {
                    "årMåned": "2021-11",
                    "klassifiserteInntekter": [
                      {
                        "beløp": 41600.0,
                        "inntektKlasse": "ARBEIDSINNTEKT"
                      }
                    ],
                    "harAvvik": false
                  },
                  {
                    "årMåned": "2021-10",
                    "klassifiserteInntekter": [
                      {
                        "beløp": 40366,
                        "inntektKlasse": "ARBEIDSINNTEKT"
                      }
                    ],
                    "harAvvik": false
                  }
                ],
                "manueltRedigert": false,
                "sisteAvsluttendeKalenderMåned": "2024-07"
              }
            }
          },
          "@forårsaket_av": {
            "id": "5ee12cde-a683-4af1-8e47-08fa1d5c1217",
            "opprettet": "2024-08-26T13:38:38.342267021",
            "event_name": "behov",
            "behov": [
              "Inntekt"
            ]
          }
        }
        """.trimIndent()
}
