package no.nav.dagpenger.behandling.mediator.api

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.TestOpplysningstyper
import no.nav.dagpenger.behandling.api.models.BarnelisteDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.PeriodeVerdiDTO
import no.nav.dagpenger.behandling.april
import no.nav.dagpenger.behandling.januar
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Avklaringspunkter
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.hendelse.Søknadstype
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingApiMapperTest {
    private val hendelse =
        SøknadInnsendtHendelse(
            meldingsreferanseId = UUIDv7.ny(),
            ident = "123123123",
            søknadId = UUIDv7.ny(),
            gjelderDato = LocalDate.now(),
            fagsakId = 1,
            opprettet = LocalDateTime.now(),
            Søknadstype.NySøknad,
        )
    private val avklaringer =
        listOf(
            Avklaring.rehydrer(
                UUIDv7.ny(),
                Avklaringkode("tittel 1", "beskrivelse ", "kanKvitteres"),
                mutableListOf(
                    Avklaring.Endring.Avbrutt(),
                ),
            ),
            Avklaring.rehydrer(
                UUIDv7.ny(),
                Avklaringkode("tittel 2", "beskrivelse ", "kanKvitteres"),
                mutableListOf(
                    Avklaring.Endring.Avklart(
                        avklartAv = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("Z123456")),
                        begrunnelse = "heia",
                    ),
                ),
            ),
            Avklaring.rehydrer(
                UUIDv7.ny(),
                Avklaringkode("tittel 3", "beskrivelse ", "kanKvitteres"),
                mutableListOf(
                    Avklaring.Endring.UnderBehandling(),
                ),
            ),
            Avklaring.rehydrer(
                UUIDv7.ny(),
                Avklaringspunkter.InntektNesteKalendermåned,
                mutableListOf(
                    Avklaring.Endring.Avklart(
                        avklartAv = Systemkilde(UUIDv7.ny(), LocalDateTime.now()),
                        begrunnelse = "heia",
                    ),
                ),
            ),
        )
    private val behandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = hendelse,
            gjeldendeOpplysninger =
                Opplysninger.med(
                    Faktum(
                        Søknadstidspunkt.prøvingsdato,
                        LocalDate
                            .now(),
                    ),
                    Faktum(
                        Verneplikt.avtjentVerneplikt,
                        true,
                    ),
                    Faktum(
                        kravTilAlder,
                        true,
                        gyldighetsperiode = Gyldighetsperiode(LocalDate.MIN, 15.januar),
                    ),
                    Faktum(
                        kravTilAlder,
                        false,
                        gyldighetsperiode = Gyldighetsperiode(16.januar),
                    ),
                    Faktum(
                        opplysningstype = Søknadstidspunkt.søknadsdato,
                        verdi =
                            LocalDate
                                .now(),
                        kilde =
                            Saksbehandlerkilde(
                                UUIDv7
                                    .ny(),
                                Saksbehandler("Z123456"),
                            ),
                    ),
                    Faktum(
                        opplysningstype = Minsteinntekt.inntekt12,
                        verdi =
                            Beløp(3000.034.toBigDecimal()),
                    ),
                    Faktum(
                        opplysningstype = TestOpplysningstyper.heltall,
                        verdi = 3,
                    ),
                    Faktum(
                        opplysningstype = TestOpplysningstyper.desimal,
                        verdi = 3.0,
                    ),
                    Faktum(
                        opplysningstype = TestOpplysningstyper.boolsk,
                        verdi = true,
                    ),
                    Faktum(
                        opplysningstype = TestOpplysningstyper.dato,
                        verdi =
                            LocalDate
                                .now(),
                    ),
                    Faktum(
                        opplysningstype = TestOpplysningstyper.beløpA,
                        verdi =
                            Beløp(1000.toBigDecimal()),
                    ),
                    Faktum(
                        opplysningstype = TestOpplysningstyper.periode,
                        verdi =
                            Periode(16.april(2025), 25.april(2025)),
                    ),
                    Faktum(
                        opplysningstype = TestOpplysningstyper.barn,
                        verdi =
                            BarnListe(
                                listOf(
                                    Barn(
                                        fornavnOgMellomnavn = "Navn",
                                        etternavn = "Navnesen",
                                        statsborgerskap = "NOR",
                                        fødselsdato =
                                            LocalDate
                                                .now(),
                                        kvalifiserer = true,
                                    ),
                                ),
                            ),
                        kilde =
                            Saksbehandlerkilde(
                                UUIDv7
                                    .ny(),
                                Saksbehandler("Z123456"),
                            ),
                    ),
                ),
            basertPå = null,
            tilstand = Behandling.TilstandType.TilGodkjenning,
            sistEndretTilstand =
                LocalDateTime
                    .now(),
            avklaringer = avklaringer,
        )

    @Test
    fun `inneholder utfall og vilkår`() {
        val behandlingDto = behandling.tilBehandlingDTO()

        behandlingDto.utfall shouldBe false

        behandlingDto.vilkår shouldHaveSize 16
        behandlingDto.vilkår.single { it.navn == "Alder" }.relevantForVedtak shouldBe true
    }

    @Test
    fun `mapper til riktig type`() {
        val behandlingDto = behandling.tilBehandlingDTO()
        // sanity check
        with(behandlingDto.opplysninger) {
            with(opplysning(TestOpplysningstyper.beløpA.navn)) {
                shouldNotBeNull()
                verdi shouldBe "1000"
                val pengeVerdi: PengeVerdiDTO = verdien.shouldBeInstanceOf()
                pengeVerdi.verdi shouldBe 1000.toBigDecimal()
                pengeVerdi.datatype shouldBe no.nav.dagpenger.behandling.api.models.DataTypeDTO.PENGER
            }
            with(opplysning(TestOpplysningstyper.boolsk.navn)) {
                shouldNotBeNull()
                verdi shouldBe "true"
                val boolsk: BoolskVerdiDTO = verdien.shouldBeInstanceOf()
                boolsk.verdi shouldBe true
                boolsk.datatype shouldBe no.nav.dagpenger.behandling.api.models.DataTypeDTO.BOOLSK
            }
            with(opplysning(TestOpplysningstyper.periode.navn)) {
                shouldNotBeNull()
                verdi shouldBe "Periode(start=2025-04-16, endInclusive=2025-04-25)"
                val boolsk: PeriodeVerdiDTO = verdien.shouldBeInstanceOf()
                boolsk.fom shouldBe 16.april(2025)
                boolsk.tom shouldBe 25.april(2025)
                boolsk.datatype shouldBe no.nav.dagpenger.behandling.api.models.DataTypeDTO.PERIODE
            }
            with(opplysning(TestOpplysningstyper.barn.navn)) {
                shouldNotBeNull()
                verdi.shouldNotBeEmpty()
                val barn: BarnelisteDTO = verdien.shouldBeInstanceOf()
                barn.verdi.shouldHaveSize(1)
                with(barn.verdi.first()) {
                    fødselsdato shouldBe java.time.LocalDate.now()
                    fornavnOgMellomnavn shouldBe "Navn"
                    etternavn shouldBe "Navnesen"
                    statsborgerskap shouldBe "NOR"
                    kvalifiserer shouldBe true
                }
            }
            with(opplysning(TestOpplysningstyper.heltall.navn)) {
                shouldNotBeNull()
                verdi shouldBe "3"
                val boolsk: HeltallVerdiDTO = verdien.shouldBeInstanceOf()
                boolsk.verdi shouldBe 3
                boolsk.datatype shouldBe no.nav.dagpenger.behandling.api.models.DataTypeDTO.HELTALL
            }
            with(opplysning(Minsteinntekt.inntekt12.navn)) {
                shouldNotBeNull()
                verdi shouldBe "3000.034"
                val pengeVerdi: PengeVerdiDTO = verdien.shouldBeInstanceOf()
                pengeVerdi.verdi shouldBe 3000.034.toBigDecimal()
                pengeVerdi.datatype shouldBe no.nav.dagpenger.behandling.api.models.DataTypeDTO.PENGER
            }
        }
    }

    private fun List<OpplysningDTO>.opplysning(navn: String) = find { it.navn == navn }
}
