package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.api.behandling.models.OpplysningerDTO
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.mediator.api.somOpplysningperiode
import no.nav.dagpenger.mediator.api.tilDataTypeDTO
import no.nav.dagpenger.mediator.api.tilOpplysningsperiodeDTO
import no.nav.dagpenger.opplysning.Behovsløserkilde
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøringsrapport
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerbegrunnelse
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.opplysning.Utledning
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.Ulid
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.DagpengerRegistrering
import no.nav.dagpenger.regel.mottak.MigrerYtelseMottak
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.dagpenger.inntekt.v1.Inntekt as InntektV1

class MigreringSimulerteOpplysningerKontraktTest {
    private val registrert = LocalDateTime.of(2026, 1, 5, 12, 0)
    private val rapid = TestRapid()
    private val ulidType =
        Opplysningstype.ulid(
            Opplysningstype.Id(UUIDv7.ny(), ULID),
            "Test-ULID",
            behovId = "test-ulid",
        )
    private val tidsenhetType =
        Opplysningstype.desimaltall(
            Opplysningstype.Id(UUIDv7.ny(), Desimaltall),
            "Timer",
            behovId = "timer",
            enhet = Enhet.Timer,
        )
    private lateinit var faktagrunnlag: Opplysninger

    @Test
    fun `simulerte opplysninger følger OpplysningerDTO-kontrakten`() {
        MigrerYtelseMottak(
            rapid,
            mockk<HendelseMottaker>(relaxed = true),
            DagpengerRegistrering().opplysningstyper,
            simulator = { _, opplysninger ->
                faktagrunnlag = opplysninger
                leggTilTestopplysninger(opplysninger)
                Regelkjøringsrapport(emptySet(), emptySet(), emptyMap(), emptySet(), emptyList())
            },
        )

        rapid.sendTestMessage(
            objectMapper.writeValueAsString(
                mapOf(
                    "@event_name" to "migrering_startet",
                    "@id" to UUIDv7.ny(),
                    "@opprettet" to registrert,
                    "migreringId" to UUIDv7.ny(),
                    "sakId" to 1,
                    "ident" to "test-ident",
                    "migreringTidspunkt" to LocalDate.of(2026, 1, 5),
                    "simulering" to true,
                ),
            ),
        )

        val melding = objectMapper.readTree(rapid.inspektør.message(0).toString())
        melding["@event_name"].asString() shouldBe "migrering_simulering_resultat"

        val dto = faktagrunnlag.tilOpplysningerDTO()
        val dtoJson = objectMapper.readTree(objectMapper.writeValueAsString(dto))
        val simulerte = melding["simulerteOpplysninger"]
        val forventet =
            dto.map { opplysning ->
                opplysning.copy(
                    perioder = opplysning.perioder.map { it.copy(type = null, kilde = null, utledetAv = null) },
                )
            }
        simulerte.values().forEach { opplysning ->
            opplysning["perioder"].values().forEach { periode ->
                periode.has("type") shouldBe false
                periode.has("kilde") shouldBe false
                periode.has("utledetAv") shouldBe false
            }
        }
        simulerte shouldBe objectMapper.readTree(objectMapper.writeValueAsString(forventet))
        objectMapper.readValue<List<OpplysningerDTO>>(simulerte.toString()) shouldBe forventet
        objectMapper.readValue<List<OpplysningerDTO>>(dtoJson.toString()) shouldBe dto
        sjekkÅpnePerioder(dtoJson, simulerte)
    }

    private fun leggTilTestopplysninger(opplysninger: Opplysninger) {
        val førstePeriode = Gyldighetsperiode(LocalDate.MIN, LocalDate.of(2025, 3, 31))
        val andrePeriode = Gyldighetsperiode(LocalDate.of(2025, 4, 1), LocalDate.MAX)
        val kilde =
            Systemkilde(
                meldingsreferanseId = UUID.fromString("019b8320-3d78-7b6f-a7c3-bc249d39a1ed"),
                opprettet = registrert,
                registrert = registrert,
            )
        val saksbehandlerkilde =
            Saksbehandlerkilde(
                meldingsreferanseId = UUIDv7.ny(),
                saksbehandler = Saksbehandler("test-saksbehandler"),
                begrunnelse = Saksbehandlerbegrunnelse("Testbegrunnelse", registrert),
                opprettet = registrert,
                registrert = registrert,
            )
        val behovsløserkilde =
            Behovsløserkilde(
                meldingsreferanseId = UUIDv7.ny(),
                opprettet = registrert,
                registrert = registrert,
            )
        val boolsk =
            Faktum(
                TestOpplysningstyper.boolsk,
                true,
                førstePeriode,
                kilde = kilde,
                opprettet = registrert,
            )
        opplysninger.leggTil(boolsk)
        opplysninger.leggTil(
            Faktum(
                TestOpplysningstyper.boolsk,
                false,
                andrePeriode,
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                TestOpplysningstyper.dato,
                LocalDate.of(1990, 7, 12),
                Gyldighetsperiode(LocalDate.of(2024, 1, 1), LocalDate.MAX),
                kilde = behovsløserkilde,
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                TestOpplysningstyper.beløpA,
                Beløp(BigDecimal("1234.56")),
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                TestOpplysningstyper.desimal,
                37.5,
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                tidsenhetType,
                12.5,
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                TestOpplysningstyper.inntektA,
                Inntekt(
                    InntektV1(
                        inntektsId = "test-inntekt-id",
                        sisteAvsluttendeKalenderMåned = YearMonth.of(2025, 11),
                        inntektsListe =
                            listOf(
                                KlassifisertInntektMåned(
                                    årMåned = YearMonth.of(2025, 11),
                                    klassifiserteInntekter =
                                        listOf(
                                            KlassifisertInntekt(
                                                BigDecimal("4200.25"),
                                                InntektKlasse.ARBEIDSINNTEKT,
                                            ),
                                        ),
                                    harAvvik = false,
                                ),
                            ),
                    ),
                ),
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                TestOpplysningstyper.barn,
                BarnListe(
                    barn =
                        listOf(
                            Barn(
                                fødselsdato = LocalDate.of(2020, 2, 3),
                                kvalifiserer = true,
                                forsørgeransvar = false,
                            ),
                        ),
                ),
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                TestOpplysningstyper.periode,
                Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14)),
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                TestOpplysningstyper.heltall,
                7,
                utledetAv = Utledning("Kontraktregel", listOf(boolsk), "testversjon"),
                kilde = saksbehandlerkilde,
                opprettet = registrert,
            ),
        )
        opplysninger.leggTil(
            Faktum(
                ulidType,
                Ulid("01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                opprettet = registrert,
            ),
        )
    }

    private fun Opplysninger.tilOpplysningerDTO(): List<OpplysningerDTO> {
        val erEgen: (no.nav.dagpenger.opplysning.Opplysning<*>) -> Boolean = this::erEgen
        return somListe().somOpplysningperiode { type, opplysninger ->
            OpplysningerDTO(
                opplysningTypeId = type.id.uuid,
                navn = type.navn,
                datatype = type.datatype.tilDataTypeDTO(),
                perioder = opplysninger.map { it.tilOpplysningsperiodeDTO(erEgen) },
            )
        }
    }

    private fun sjekkÅpnePerioder(
        dtoGrupper: JsonNode,
        simulerteGrupper: JsonNode,
    ) {
        val typeId =
            TestOpplysningstyper.boolsk.id.uuid
                .toString()
        val dtoPerioder =
            dtoGrupper
                .values()
                .single { it["opplysningTypeId"].asString() == typeId }["perioder"]
                .values()
                .associateBy { it["id"].asString() }
        val simulertePerioder =
            simulerteGrupper
                .values()
                .single { it["opplysningTypeId"].asString() == typeId }["perioder"]
                .values()
                .associateBy { it["id"].asString() }

        val åpenFra = dtoPerioder.values.single { it.path("gyldigTilOgMed").asString() == "2025-03-31" }
        val åpenTil = dtoPerioder.values.single { it.path("gyldigFraOgMed").asString() == "2025-04-01" }
        simulertePerioder
            .getValue(åpenFra["id"].asString())
            .has("gyldigFraOgMed") shouldBe false
        simulertePerioder
            .getValue(åpenTil["id"].asString())
            .has("gyldigTilOgMed") shouldBe false
    }
}
