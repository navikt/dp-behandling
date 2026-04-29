package no.nav.dagpenger.brev

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID
import kotlin.test.Test

class BrevmalSerialiseringTest {
    private val objectMapper =
        jacksonObjectMapper().apply {
            enable(SerializationFeature.INDENT_OUTPUT)
        }

    @Test
    fun `brevmal med alle trigger-typer kan serialiseres og deserialiseres`() {
        val opplysningsTypeId = UUID.fromString("0194881f-940b-76ff-acf5-ba7bcb367237")

        val brevmal =
            Brevmal(
                id = UUID.fromString("01948820-0000-7000-8000-000000000001"),
                navn = "Dagpengebrev - ny søknad",
                maltekster =
                    listOf(
                        Maltekst(
                            id = UUID.fromString("01948820-0000-7000-8000-000000000010"),
                            trigger = Trigger.Avgjørelse("Innvilgelse"),
                            tekst = "Innvilgelse av dagpenger",
                            plassering = Plassering.OVERSKRIFT,
                            rekkefølge = 1,
                        ),
                        Maltekst(
                            id = UUID.fromString("01948820-0000-7000-8000-000000000011"),
                            trigger = Trigger.Alltid,
                            tekst = "Vi har behandlet søknaden din om dagpenger.",
                            plassering = Plassering.INNLEDNING,
                            rekkefølge = 1,
                        ),
                        Maltekst(
                            id = UUID.fromString("01948820-0000-7000-8000-000000000012"),
                            trigger = Trigger.OpplysningFinnes(opplysningsTypeId),
                            tekst = "Vi har vurdert om du oppfyller alderskravet.",
                            plassering = Plassering.VILKÅR,
                            rekkefølge = 1,
                        ),
                        Maltekst(
                            id = UUID.fromString("01948820-0000-7000-8000-000000000013"),
                            trigger = Trigger.OpplysningVerdi(opplysningsTypeId, "true"),
                            tekst = "Du oppfyller kravet til alder.",
                            plassering = Plassering.VILKÅR,
                            rekkefølge = 2,
                        ),
                    ),
            )

        val json = objectMapper.writeValueAsString(brevmal)
        val deserialisert: Brevmal = objectMapper.readValue(json)

        deserialisert shouldBe brevmal
        deserialisert.maltekster[0].trigger.shouldBeInstanceOf<Trigger.Avgjørelse>()
        deserialisert.maltekster[1].trigger.shouldBeInstanceOf<Trigger.Alltid>()
        deserialisert.maltekster[2].trigger.shouldBeInstanceOf<Trigger.OpplysningFinnes>()
        deserialisert.maltekster[3].trigger.shouldBeInstanceOf<Trigger.OpplysningVerdi>()
    }

    @Test
    fun `trigger-typer serialiseres med type-discriminator`() {
        val json = objectMapper.writeValueAsString(Trigger.Alltid)
        json.contains("\"type\"") shouldBe true
        json.contains("\"alltid\"") shouldBe true

        val avgjørelseJson = objectMapper.writeValueAsString(Trigger.Avgjørelse("Innvilgelse"))
        avgjørelseJson.contains("\"avgjørelse\"") shouldBe true
        avgjørelseJson.contains("\"Innvilgelse\"") shouldBe true
    }

    @Test
    fun `brevmal JSON roundtrip bevarer alle felter`() {
        val brevmal =
            Brevmal(
                navn = "Testmal",
                maltekster =
                    listOf(
                        Maltekst(
                            trigger = Trigger.Alltid,
                            tekst = "Hei {{ident}}, din sats er {{Dagsats}} kr.",
                            plassering = Plassering.INNLEDNING,
                            rekkefølge = 1,
                        ),
                    ),
            )

        val json = objectMapper.writeValueAsString(brevmal)
        val deserialisert: Brevmal = objectMapper.readValue(json)

        deserialisert.navn shouldBe "Testmal"
        deserialisert.maltekster[0].tekst shouldBe "Hei {{ident}}, din sats er {{Dagsats}} kr."
        deserialisert.maltekster[0].plassering shouldBe Plassering.INNLEDNING
        deserialisert.maltekster[0].rekkefølge shouldBe 1
    }
}
