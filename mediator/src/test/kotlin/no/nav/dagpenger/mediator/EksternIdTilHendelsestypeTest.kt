package no.nav.dagpenger.mediator

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.hendelser.ArbeidssøkerperiodeId
import no.nav.dagpenger.modell.hendelser.EksternId
import no.nav.dagpenger.modell.hendelser.FerietilleggId
import no.nav.dagpenger.modell.hendelser.KlageFørsteinstansId
import no.nav.dagpenger.modell.hendelser.KlageKlageinstansId
import no.nav.dagpenger.modell.hendelser.KlageTrygderettenId
import no.nav.dagpenger.modell.hendelser.ManuellId
import no.nav.dagpenger.modell.hendelser.MeldekortId
import no.nav.dagpenger.modell.hendelser.OmgjøringId
import no.nav.dagpenger.modell.hendelser.SamordningId
import no.nav.dagpenger.modell.hendelser.SøknadId
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class EksternIdTilHendelsestypeTest {
    @Test
    fun `hendelseType returnerer korrekt string for alle EksternId-typer`() {
        val søknadId = SøknadId(UUIDv7.ny())
        søknadId.hendelseType() shouldBe "Søknad"

        val meldekortId = MeldekortId("12345")
        meldekortId.hendelseType() shouldBe "Meldekort"

        val manuellId = ManuellId(UUIDv7.ny())
        manuellId.hendelseType() shouldBe "Manuell"

        val omgjøringId = OmgjøringId(UUIDv7.ny())
        omgjøringId.hendelseType() shouldBe "Omgjøring"

        val ferietilleggId = FerietilleggId(UUIDv7.ny())
        ferietilleggId.hendelseType() shouldBe "Ferietillegg"

        val arbeidssøkerperiodeId = ArbeidssøkerperiodeId(UUIDv7.ny())
        arbeidssøkerperiodeId.hendelseType() shouldBe "Arbeidssøkerperiode"

        val samordningId = SamordningId(UUIDv7.ny())
        samordningId.hendelseType() shouldBe "Samordning"
    }

    @Test
    fun `hendelseType returnerer korrekt string for KlageId-typer`() {
        val klageFørsteinstansId = KlageFørsteinstansId(UUIDv7.ny())
        klageFørsteinstansId.hendelseType() shouldBe "KlageFørsteinstans"

        val klageKlageinstansId = KlageKlageinstansId("ABC123")
        klageKlageinstansId.hendelseType() shouldBe "KlageKlageinstans"

        val klageTrygderettenId = KlageTrygderettenId("XYZ789")
        klageTrygderettenId.hendelseType() shouldBe "KlageTrygderetten"
    }

    @Test
    fun `hendelseType gir distinkt navn for ulike EksternId-typer`() {
        val ids =
            listOf(
                SøknadId(UUIDv7.ny()) as EksternId<*>,
                MeldekortId("12345") as EksternId<*>,
                ManuellId(UUIDv7.ny()) as EksternId<*>,
                OmgjøringId(UUIDv7.ny()) as EksternId<*>,
                FerietilleggId(UUIDv7.ny()) as EksternId<*>,
                ArbeidssøkerperiodeId(UUIDv7.ny()) as EksternId<*>,
                SamordningId(UUIDv7.ny()) as EksternId<*>,
                KlageFørsteinstansId(UUIDv7.ny()) as EksternId<*>,
                KlageKlageinstansId("ABC123") as EksternId<*>,
                KlageTrygderettenId("XYZ789") as EksternId<*>,
            )

        val hendelseTyper = ids.map { it.hendelseType() }
        hendelseTyper.distinct().size shouldBe hendelseTyper.size
    }

    @Test
    fun `hendelseType gir menneskelesbar navn som skiller seg fra type-propertien`() {
        val søknadId = SøknadId(UUIDv7.ny())
        // type-propertien returnerer klassens enkle navn
        søknadId.type shouldBe "SøknadId"
        // hendelseType returnerer en menneskelesbar hendelsestype
        søknadId.hendelseType() shouldBe "Søknad"

        val meldekortId = MeldekortId("12345")
        meldekortId.type shouldBe "MeldekortId"
        meldekortId.hendelseType() shouldBe "Meldekort"

        val klageKlageinstansId = KlageKlageinstansId("ABC123")
        klageKlageinstansId.type shouldBe "KlageKlageinstansId"
        klageKlageinstansId.hendelseType() shouldBe "KlageKlageinstans"
    }
}
