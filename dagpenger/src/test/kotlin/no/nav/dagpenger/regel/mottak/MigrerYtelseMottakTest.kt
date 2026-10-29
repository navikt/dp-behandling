package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.modell.hendelser.MigreringId
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
import no.nav.dagpenger.regel.regelsett.fastsetting.Egenandel
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MigrerYtelseMottakTest {
    private val påkrevdeOpplysninger =
        mapOf<Opplysningstype<*>, Any>(
            Alderskrav.fødselsdato to "1990-01-01",
            Dagpengegrunnlag.grunnlag to 310630,
            Opphold.oppfyllerKravet to true,
            Dagpengeperiode.ordinærPeriode to 104,
            DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg to 1500,
            DagpengenesStørrelse.antallBarn to 0,
            Vanligarbeidstid.fastsattVanligArbeidstid to 37.5,
            Egenandel.egenandel to 0,
            TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon to 50.0,
            Samordning.samordnetDagsats to 1500,
            Samordning.skalSamordnes to false,
            DagpengenesStørrelse.harSamordnet to false,
            Samordning.utfallEtterSamordning to true,
            SamordingUtenforFolketrygden.dagsatsSamordnetUtenforFolketrygden to 1500,
            SamordingUtenforFolketrygden.andreYtelser to false,
        )
    private val jsonMapper = JsonMapper.builder().build()
    private val ekstra = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "ekstra")
    private val rapid = TestRapid()
    private val migreringId = UUIDv7.ny()
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)

    private val mottak =
        MigrerYtelseMottak(
            rapidsConnection = rapid,
            hendelseMottaker = hendelseMottaker,
            opplysningstyper = DagpengerRegistrering().opplysningstyper,
        )

    @Test
    fun `returnerer alle manglende pakrevde opplysningstyper`() {
        val packet = valideringsmelding(emptyMap())

        mottak.validerOpplysninger(packet) shouldBe
            påkrevdeOpplysninger.keys.toList()
    }

    @TestFactory
    fun `returnerer bare opplysningstypen som mangler`() =
        påkrevdeOpplysninger.keys.map { manglendeType ->
            dynamicTest(manglendeType.behovId) {
                val packet = valideringsmelding(testopplysninger(uten = manglendeType))

                mottak.validerOpplysninger(packet) shouldBe listOf(manglendeType)
            }
        }

    @Test
    fun `returnerer tom liste nar alle pakrevde opplysningstyper finnes i objektet`() {
        val packet = valideringsmelding(testopplysninger())

        mottak.validerOpplysninger(packet) shouldBe emptyList()
    }

    @Test
    fun `returnerer tom liste nar alle pakrevde opplysningstyper finnes i listen`() {
        val packet =
            JsonMessage
                .newMessage(
                    "migrering_startet",
                    mapOf(
                        "opplysninger" to
                            påkrevdeOpplysninger.map { (type, verdi) ->
                                mapOf(
                                    "uuid" to type.id.uuid.toString(),
                                    "verdi" to verdi,
                                    "gyldigFraOgMed" to "2024-10-04",
                                )
                            },
                    ),
                ).apply { interestedIn("opplysninger") }

        mottak.validerOpplysninger(packet) shouldBe emptyList()
    }

    @Test
    fun `publiserer avbrudd nar pakrevde opplysninger mangler`() {
        rapid.sendTestMessage(melding(opplysninger = "{}"))

        val avbrudd = rapid.inspektør.message(0)
        avbrudd["@event_name"].asString() shouldBe "migrering_feilet"
        avbrudd["sakId"].asInt() shouldBe 13699879
        avbrudd["arsak"]["kode"].asString() shouldBe "MANGLENDE_OPPLYSNINGER"
        avbrudd["arsak"]["melding"].asString() shouldBe "Mangler påkrevde opplysninger"
        avbrudd["arsak"]["detaljer"].values().map { it.asString() } shouldBe
            påkrevdeOpplysninger.keys.map { it.behovId }
        rapid.inspektør.size shouldBe 1
        verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
    }

    @TestFactory
    fun `publiserer bare manglende pakrevd opplysning i avbruddet`() =
        påkrevdeOpplysninger.keys.map { manglendeType ->
            dynamicTest(manglendeType.behovId) {
                rapid.reset()
                rapid.sendTestMessage(melding(opplysninger = jsonMapper.writeValueAsString(testopplysninger(uten = manglendeType))))

                val avbrudd = rapid.inspektør.message(0)
                avbrudd["@event_name"].asString() shouldBe "migrering_feilet"
                avbrudd["sakId"].asInt() shouldBe 13699879
                avbrudd["arsak"]["detaljer"].values().map { it.asString() } shouldBe listOf(manglendeType.behovId)
                rapid.inspektør.size shouldBe 1
                verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
            }
        }

    private fun testopplysninger(uten: Opplysningstype<*>? = null): Map<String, Any> =
        påkrevdeOpplysninger
            .filterKeys { it != uten }
            .mapKeys { (type, _) -> type.behovId }

    @Test
    fun `avbruddshjelperen serialiserer arsaker med detaljer`() {
        val context = mockk<MessageContext>(relaxed = true)
        val publiserte = mutableListOf<String>()
        val detaljer = listOf("grunnlag", "opphold")

        context.avbrytMigrering(
            123,
            Migreringsavbrudd(kode = "ANNEN_ÅRSAK", melding = "En annen årsak", detaljer = detaljer),
        )
        context.avbrytMigrering(
            123,
            Migreringsavbrudd(kode = "ANNEN_ÅRSAK", melding = "En annen årsak", detaljer = detaljer),
        )

        verify(exactly = 2) { context.publish(capture(publiserte)) }
        val avbrudd = JsonMapper.builder().build().readTree(publiserte.first())
        avbrudd
            .properties()
            .toList()
            .map { it.key }
            .filterNot { it.startsWith("system_") }
            .toSet() shouldBe
            setOf("@event_name", "@id", "@opprettet", "sakId", "arsak")
        avbrudd["@event_name"].asString() shouldBe "migrering_feilet"
        avbrudd["sakId"].asInt() shouldBe 123
        UUID.fromString(avbrudd["@id"].asString())
        LocalDateTime.parse(avbrudd["@opprettet"].asString())
        avbrudd["arsak"]
            .properties()
            .toList()
            .map { it.key }
            .toSet() shouldBe
            setOf("kode", "melding", "detaljer")
        avbrudd["arsak"]["kode"].asString() shouldBe "ANNEN_ÅRSAK"
        avbrudd["arsak"]["melding"].asString() shouldBe "En annen årsak"
        avbrudd["arsak"]["detaljer"].values().map { it.asString() } shouldBe detaljer
        val andreAvbrudd = JsonMapper.builder().build().readTree(publiserte.last())
        (avbrudd["@id"].asString() != andreAvbrudd["@id"].asString()) shouldBe true
    }

    private fun valideringsmelding(opplysninger: Map<String, Any>): JsonMessage =
        JsonMessage
            .newMessage("migrering_startet", mapOf("opplysninger" to opplysninger))
            .apply { interestedIn("opplysninger") }

    @Test
    fun `bruker migreringId fra meldingen som ekstern id`() {
        rapid.sendTestMessage(melding(opplysninger = jsonMapper.writeValueAsString(testopplysninger())))

        val hendelse = slot<MigrerYtelseHendelse>()
        verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }

        hendelse.captured.eksternId shouldBe MigreringId(migreringId)
    }

    @Test
    fun `oppretter behandling med kontraktsfelter og tilleggsopplysninger`() {
        rapid.sendTestMessage(melding(opplysninger = jsonMapper.writeValueAsString(testopplysninger())))

        rapid.inspektør.size shouldBe 0
        val hendelse = slot<MigrerYtelseHendelse>()
        verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }

        val behandling =
            hendelse.captured
                .behandling(null, TemporalCollection())
                .shouldBeInstanceOf<StartHendelseResultat.Opprettet>()
                .behandling
        val opplysninger = behandling.opplysninger()

        opplysninger.finnOpplysning(DagpengenesStørrelse.antallBarn).verdi shouldBe 0
        opplysninger.finnOpplysning(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg).verdi shouldBe Beløp(1500)
        opplysninger.finnOpplysning(Dagpengegrunnlag.grunnlag).verdi shouldBe Beløp(310630)
    }

    @Test
    fun `sender inn en ekte melding og sjekker at den blir opprettet`() {
        rapid.sendTestMessage(ekteMelding())

        rapid.inspektør.size shouldBe 0
        val hendelse = slot<MigrerYtelseHendelse>()
        verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }

        val behandling =
            hendelse.captured
                .behandling(null, TemporalCollection())
                .shouldBeInstanceOf<StartHendelseResultat.Opprettet>()
                .behandling
        val opplysninger = behandling.opplysninger()

        opplysninger.finnOpplysning(DagpengenesStørrelse.antallBarn).verdi shouldBe 0
        opplysninger.finnOpplysning(Dagpengeperiode.antallStønadsdager).verdi shouldBe 520
        opplysninger.finnOpplysning(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg).verdi shouldBe Beløp(1500)
        opplysninger.finnOpplysning(Dagpengegrunnlag.grunnlag).verdi shouldBe Beløp(310630)
    }

    @Disabled
    @Test
    fun `oppretter faktum fra opplysningsliste i migrering_startet`() {
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

        rapid.sendTestMessage(melding(opplysninger = opplysning, eventName = "migrering_startet"))

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

//    @Test
//    fun `kan ikke oppgi samme opplysning både eksplisitt og som tilleggsopplysning`() {
//        rapid.sendTestMessage(
//            melding(
//                opplysninger =
//                    """{ "${DagpengenesStørrelse.antallBarn.behovId}": 3, "${Dagpengegrunnlag.grunnlag.behovId}": 310630, "${Opphold.oppfyllerKravet.behovId}": true }""",
//            ),
//        )
//        val hendelse = slot<MigrerYtelseHendelse>()
//        verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }
//        shouldThrow<IllegalArgumentException> {
//            hendelse.captured.behandling(null, TemporalCollection())
//        }
//    }

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
                    eventName = "migrering_startet",
                ),
            )
        }
    }

    private fun melding(
        opplysninger: String,
        eventName: String = "migrering_startet",
    ) = """
        {
          "@event_name": "$eventName",
          "sakId": 13699879,
          "migreringId": "$migreringId",
          "ident": "12345678910",
          "innvilgetFraOgMed": "${LocalDate.of(2026, 1, 5)}",
          "opplysninger": $opplysninger
        }
        """.trimIndent()

    private fun ekteMelding() =
        """
        {
          "@event_name": "migrering_startet",
          "sakId": 13699879,
          "migreringId": "$migreringId",
          "innvilgetFraOgMed": "${LocalDate.of(2026, 1, 5)}",
          "initiertAv": "Z990798",
          "opplysninger": [
            {
              "navn": "Dagsats med barnetillegg etter samordning og 90 % regel",
              "verdi": 1500,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9428-74d5-b160-f63a4c61a24f"
            },
            {
              "navn": "Antall barn som gir rett til barnetillegg",
              "verdi": 0,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9428-74d5-b160-f63a4c61a23c"
            },
            {
              "navn": "Fastsatt arbeidstid per uke før tap",
              "verdi": 37.5,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9435-72a8-b1ce-9575cbc2a76a"
            },
            {
              "navn": "Egenandel",
              "verdi": 0,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-943f-78d9-b874-00a4944c54ef"
            },
            {
              "navn": "Krav til prosentvis tap av arbeidstid",
              "verdi": 50.0,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9435-72a8-b1ce-9575cbc2a762"
            },
            {
              "navn": "Samordnet dagsats uten barnetillegg",
              "verdi": 1500,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9434-79e8-a64d-1a23cc5d86eb"
            },
            {
              "navn": "Medlem har reduserte ytelser fra folketrygden (Samordning)",
              "verdi": false,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9434-79e8-a64d-1a23cc5d86ea"
            },
            {
              "navn": "Utfall etter samordning",
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9434-79e8-a64d-1a23cc5d86ef"
            },
            {
              "navn": "OppgittAndreYtelserUtenforNav",
              "verdi": false,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-942e-7cb0-aa59-05ea449d88e0"
            },
            {
              "navn": "Medlem i folketrygd",
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9443-72b4-8b30-5f6cdb24d54c"
            },
            {
              "navn": "fødselsdato",
              "verdi": "2002-01-01",
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-940b-76ff-acf5-ba7bcb367233"
            },
            {
              "navn": "antall stønadsuker",
              "verdi": 104,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-943d-77a7-969c-147999f15459"
            },
            {
              "navn": "antallStønadsdager",
              "verdi": 520,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-943d-77a7-969c-147999f15457"
            },
            {
              "navn": "Kravet til opphold i Norge er oppfylt",
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9443-72b4-8b30-5f6cdb24d54e"
            },
            {
              "navn": "Dagpengegrunnlag",
              "verdi": 310630,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9410-7481-b263-4606fdd10cbd"
            },
            {
              "navn": "Siste avsluttende kalendermåned",
              "verdi": "2024-08-31",
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9414-7823-8d29-0e25b7feb7d0"
            },
            {
              "navn": "Oppfyller kravet til heltid- og deltidsarbeid",
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9442-707b-a6ee-e96c06877bd8"
            },
            {
              "navn": "Oppfyller kravet til mobilitet",
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9442-707b-a6ee-e96c06877bdb"
            },
            {
              "navn": "Ukessats med barnetillegg etter samordning",
              "verdi": 3730,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9428-74d5-b160-f63a4c61a24e"
            },
            {
              "navn": "Avrundet ukessats med barnetillegg før samordning",
              "verdi": 3730,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9428-74d5-b160-f63a4c61a240"
            },
            {
              "navn": "Har samordnet",
              "verdi": false,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9428-74d5-b160-f63a4c61a250"
            },
            {
              "navn": "Utbetalt inntekt periode 1",
              "verdi": 120000,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9410-7481-b263-4606fdd10cad"
            },
            {
              "navn": "Utbetalt inntekt periode 2",
              "verdi": 60000,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9410-7481-b263-4606fdd10cae"
            },
            {
              "navn": "Utbetalt inntekt periode 3",
              "verdi": 14000000,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-9410-7481-b263-4606fdd10caf"
            },
            {
              "navn": "Oppfyller kravet til alder",
              "verdi": true,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-940b-76ff-acf5-ba7bcb367237"
            },
            {
              "navn": "Dagsats uten barnetillegg samordnet",
              "verdi": 746,
              "gyldigFraOgMed": "2024-10-04",
              "uuid": "0194881f-942f-7bde-ab16-68ffd19e9a33"
            },
            {
              "navn": "Oppfyller kravet til å være registrert som arbeidssøker",
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
          "@event_name" : "migrering_startet",
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
