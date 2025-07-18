package no.nav.dagpenger.behandling.mediator.api

import com.fasterxml.jackson.databind.SerializationFeature
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.mockk
import no.nav.dagpenger.behandling.api.models.PeriodeDTO
import no.nav.dagpenger.behandling.api.models.VerdenbesteklumpmeddataDTO
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.mai
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Companion.somOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.uuid.UUIDv7
import org.approvaltests.Approvals
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class KlumpTest {
    private val ident = "123123123"
    private val behandlingId: UUID = UUID.fromString("0198132a-4d87-7622-8fd3-9aa4558b8538")
    private val søknadId: UUID = UUID.fromString("0198132a-4d99-701c-b44c-c24ddcbb2801")

    @Test
    fun `innvilgelse med et vilkår, vurdert i en periode`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 10.mai(2025)),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 1
        klump.rettighetsperioder.shouldContainExactly(PeriodeDTO(1.mai(2025), 10.mai(2025)))

        godkjennJSON(klump)
    }

    @Test
    fun `innvilgelse med et vilkår, vurdert likt i flere perioder`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 10.mai(2025)),
                kravTilAlder.periode(11.mai(2025), 20.mai(2025)),
                kravTilAlder.periode(21.mai(2025), 30.mai(2025)),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 1
        klump.rettighetsperioder.shouldContainExactly(PeriodeDTO(1.mai(2025), 30.mai(2025)))

        godkjennJSON(klump)
    }

    @Test
    fun `innvilgelse med et vilkår, vurdert likt i flere perioder som har MAX`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 10.mai(2025)),
                kravTilAlder.periode(11.mai(2025), 20.mai(2025)),
                kravTilAlder.periode(21.mai(2025), LocalDate.MAX),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 1
        klump.rettighetsperioder.shouldContainExactly(PeriodeDTO(1.mai(2025), LocalDate.MAX))

        godkjennJSON(klump)
    }

    @Test
    fun `innvilgelse med et vilkår, vurdert ulikt i flere perioder`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 10.mai(2025)),
                kravTilAlder.periode(11.mai(2025), 20.mai(2025), false),
                kravTilAlder.periode(21.mai(2025), 30.mai(2025)),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 2
        klump.rettighetsperioder.shouldContainExactly(
            PeriodeDTO(1.mai(2025), 10.mai(2025)),
            PeriodeDTO(21.mai(2025), 30.mai(2025)),
        )

        godkjennJSON(klump)
    }

    @Test
    fun `innvilgelse med et vilkår, men har ett vilkår som ikke relevant i perioden med false`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 10.mai(2025)),
                minsteinntekt.periode(1.mai(2025), 20.mai(2025), false, false),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 1
        klump.rettighetsperioder.shouldContainExactly(PeriodeDTO(1.mai(2025), 10.mai(2025)))

        godkjennJSON(klump)
    }

    @Test
    fun `innvilgelse med et vilkår, vurdert ulikt i flere perioder med opphold med hull`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 10.mai(2025)),
                kravTilAlder.periode(21.mai(2025), 30.mai(2025)),
                kravTilAlder.periode(5.juni(2025), 5.juni(2028)),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 3
        klump.rettighetsperioder.shouldContainExactly(
            PeriodeDTO(1.mai(2025), 10.mai(2025)),
            PeriodeDTO(21.mai(2025), 30.mai(2025)),
            PeriodeDTO(5.juni(2025), 5.juni(2028)),
        )

        godkjennJSON(klump)
    }

    @Test
    fun `innvilgelse som til sammen utgjør en periode med innvilget`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 10.mai(2025)),
                kravTilAlder.periode(21.mai(2025), 30.mai(2025)),
                kravTilAlder.periode(5.juni(2025), 5.juni(2028)),
                minsteinntekt.periode(1.mai(2025), 5.juni(2025)),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 1
        klump.rettighetsperioder.shouldContainExactly(
            PeriodeDTO(1.mai(2025), 5.juni(2028)),
        )

        godkjennJSON(klump)
    }

    @Test
    fun `innvilgelse med et vilkår, vurdert ulikt i flere perioder med opphold, uten hull`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 10.mai(2025)),
                kravTilAlder.periode(11.mai(2025), 20.mai(2025), false),
                kravTilAlder.periode(21.mai(2025), 30.mai(2025)),
                kravTilAlder.periode(31.mai(2025), 4.juni(2025), false),
                kravTilAlder.periode(5.juni(2025), 5.juni(2028)),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 3
        klump.rettighetsperioder.shouldContainExactly(
            PeriodeDTO(1.mai(2025), 10.mai(2025)),
            PeriodeDTO(21.mai(2025), 30.mai(2025)),
            PeriodeDTO(5.juni(2025), 5.juni(2028)),
        )

        godkjennJSON(klump)
    }

    @Test
    fun `innvilgelse med flere vilkår, oppfylt på ulik startdato`() {
        val resultat =
            resultat(
                kravTilAlder.periode(1.mai(2025), 30.mai(2025)),
                minsteinntekt.periode(1.mai(2025), 4.mai(2025), false),
                minsteinntekt.periode(5.mai(2025), 30.mai(2025)),
            )

        val klump = resultat.tilKlumpDTO(ident)
        klump.rettighetsperioder shouldHaveSize 1
        klump.rettighetsperioder.shouldContainExactly(PeriodeDTO(5.mai(2025), 30.mai(2025)))

        godkjennJSON(klump)
    }

    private fun Opplysningstype<Boolean>.periode(
        fraOgMed: LocalDate,
        tilOgMed: LocalDate? = null,
        vurdering: Boolean = true,
        erRelevant: Boolean = true,
    ): Opplysning<*> =
        Faktum(
            this,
            vurdering,
            tilOgMed?.let { Gyldighetsperiode(fraOgMed, tilOgMed) } ?: Gyldighetsperiode(fraOgMed),
        ).also { it.erRelevant(erRelevant) }

    private fun resultat(vararg vurderinger: Opplysning<*>): Behandling.Resultat =
        Behandling.Resultat(
            behandlingId = behandlingId,
            basertPåBehandlinger = emptyList(),
            utfall = true,
            virkningsdato = LocalDate.now(),
            behandlingAv =
                SøknadInnsendtHendelse(
                    meldingsreferanseId = UUIDv7.ny(),
                    ident = ident,
                    søknadId = søknadId,
                    gjelderDato = LocalDate.now(),
                    fagsakId = 1,
                    opprettet = LocalDateTime.now(),
                ),
            opplysninger = vurderinger.toList().somOpplysninger(),
            automatiskBehandlet = false,
            godkjentAv = mockk(),
            besluttetAv = mockk(),
        )

    private fun godkjennJSON(klump: VerdenbesteklumpmeddataDTO) {
        val json =
            objectMapper
                .apply {
                    enable(SerializationFeature.INDENT_OUTPUT)
                }.writeValueAsString(klump)

        Approvals.verify(maskUUIDs(json))
    }

    private fun maskUUIDs(json: String): String {
        // Regex matches UUIDs (standard format)
        return json.replace(
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"),
            "00000000-0000-0000-0000-000000000000",
        )
    }
}
