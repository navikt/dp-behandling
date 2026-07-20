package no.nav.dagpenger.modell.hendelser

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class EksternIdTest {
    @Test
    fun `likhet test`() {
        val søknadId = SøknadId(UUIDv7.ny())
        søknadId shouldBeEqual søknadId
        søknadId shouldNotBeEqual SøknadId(UUIDv7.ny())
        søknadId shouldNotBeEqual Any()
        søknadId.hashCode() shouldBeEqual søknadId.hashCode()
        søknadId.hashCode() shouldNotBeEqual SøknadId(UUIDv7.ny()).hashCode()
        søknadId.hashCode() shouldNotBeEqual Any().hashCode()
    }

    @Test
    fun `hendelseType returnerer klassens navn`() {
        val søknadId = SøknadId(UUIDv7.ny())
        søknadId.type shouldBe "SøknadId"

        val meldekortId = MeldekortId("12345")
        meldekortId.type shouldBe "MeldekortId"

        val manuellId = ManuellId(UUIDv7.ny())
        manuellId.type shouldBe "ManuellId"

        val omgjøringId = OmgjøringId(UUIDv7.ny())
        omgjøringId.type shouldBe "OmgjøringId"

        val ferietilleggId = FerietilleggId(UUIDv7.ny())
        ferietilleggId.type shouldBe "FerietilleggId"

        val arbeidssøkerperiodeId = ArbeidssøkerperiodeId(UUIDv7.ny())
        arbeidssøkerperiodeId.type shouldBe "ArbeidssøkerperiodeId"

        val samordningId = SamordningId(UUIDv7.ny())
        samordningId.type shouldBe "SamordningId"
    }

    @Test
    fun `hendelseType for klageIdeer`() {
        val klageFørsteinstansId = KlageFørsteinstansId(UUIDv7.ny())
        klageFørsteinstansId.type shouldBe "KlageFørsteinstansId"

        val klageKlageinstansId = KlageKlageinstansId("ABC123")
        klageKlageinstansId.type shouldBe "KlageKlageinstansId"

        val klageTrygderettenId = KlageTrygderettenId("XYZ789")
        klageTrygderettenId.type shouldBe "KlageTrygderettenId"
    }

    @Test
    fun `datatype returnerer riktig type`() {
        val søknadId = SøknadId(UUIDv7.ny())
        søknadId.datatype shouldBe "UUID"

        val meldekortId = MeldekortId("12345")
        meldekortId.datatype shouldBe "String"

        val klageKlageinstansId = KlageKlageinstansId("ABC123")
        klageKlageinstansId.datatype shouldBe "String"
    }

    @Test
    fun `toString inneholder klassens navn og id`() {
        val uuid = UUIDv7.ny()
        val søknadId = SøknadId(uuid)
        søknadId.toString() shouldBe "SøknadId($uuid)"

        val meldekortId = MeldekortId("12345")
        meldekortId.toString() shouldBe "MeldekortId(12345)"
    }

    @Test
    fun `kontekstMap returnerer riktig struktur`() {
        val søknadId = SøknadId(UUIDv7.ny())
        val søknadKontekst = søknadId.kontekstMap()
        søknadKontekst.keys shouldBe setOf("søknadId", "søknad_uuid")

        val meldekortId = MeldekortId("12345")
        val meldekortKontekst = meldekortId.kontekstMap()
        meldekortKontekst.keys shouldBe setOf("meldekortId")
        meldekortKontekst["meldekortId"] shouldBe "12345"
    }

    @Test
    fun `fromString konstruerer riktig EksternId fra type og id-streng`() {
        val søknadId = EksternId.fromString("SøknadId", UUIDv7.ny().toString())
        søknadId shouldBe EksternId.fromString("SøknadId", søknadId.id.toString())

        val meldekortId = EksternId.fromString("MeldekortId", "12345")
        (meldekortId as MeldekortId).id shouldBe "12345"

        val manuellId = EksternId.fromString("ManuellId", UUIDv7.ny().toString())
        manuellId::class.simpleName shouldBe "ManuellId"

        val ferietilleggId = EksternId.fromString("FerietilleggId", UUIDv7.ny().toString())
        ferietilleggId::class.simpleName shouldBe "FerietilleggId"

        val klageKlageinstansId = EksternId.fromString("KlageKlageinstansId", "ABC123")
        (klageKlageinstansId as KlageKlageinstansId).id shouldBe "ABC123"
    }

    @Test
    fun `fromString kaster exception for ukjent type`() {
        try {
            EksternId.fromString("UkjentType", "12345")
            throw AssertionError("Forventet IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            e.message shouldBe "Ukjent idType: UkjentType"
        }
    }
}
