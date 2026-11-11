package no.nav.dagpenger.regel.hendelse

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.hendelser.AktivitetType
import no.nav.dagpenger.modell.hendelser.Dag
import no.nav.dagpenger.modell.hendelser.Meldekort
import no.nav.dagpenger.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.modell.hendelser.MeldekortId
import no.nav.dagpenger.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.Opprettet
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.hendelse.BeregnMeldekortHendelse.Companion.meldekortMedUtdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.godkjentUnntakForUtdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.tarUtdanning
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class BeregnMeldekortHendelseTest {
    private val fom = LocalDate.of(2026, 1, 5)
    private val tom = fom.plusDays(13)
    private val førsteUtdanningsdag = fom.plusDays(2)

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `beholder eksisterende true og gyldighet uavhengig av godkjent unntak`(godkjentUnntak: Boolean) {
        val eksisterende = Faktum(tarUtdanning, true, Gyldighetsperiode(fom, tom))
        val behandling =
            opprettBehandling(
                Opplysninger.med(
                    eksisterende,
                    Faktum(godkjentUnntakForUtdanning, godkjentUnntak, Gyldighetsperiode(fom)),
                ),
            )

        behandling.opplysninger.kunEgne.finnAlle(tarUtdanning) shouldBe emptyList()
        val utdanning = behandling.opplysninger.finnAlle(tarUtdanning).single()
        utdanning.id shouldBe eksisterende.id
        utdanning.gyldighetsperiode shouldBe eksisterende.gyldighetsperiode
        behandling.avklaringer().any { it.kode == meldekortMedUtdanning } shouldBe !godkjentUnntak
        behandling.behandler.opplysninger.finnAlle(tarUtdanning) shouldBe emptyList()
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [false])
    fun `legger til true fra utdanningsdagen når opplysningen mangler eller er false`(eksisterendeVerdi: Boolean?) {
        val opplysninger =
            Opplysninger.med(
                listOfNotNull(eksisterendeVerdi?.let { Faktum(tarUtdanning, it, Gyldighetsperiode(fom)) }),
            )

        val behandling = opprettBehandling(opplysninger)

        val utdanning =
            behandling.opplysninger.kunEgne
                .finnAlle(tarUtdanning)
                .single()
        utdanning.verdi shouldBe true
        utdanning.gyldighetsperiode shouldBe Gyldighetsperiode(førsteUtdanningsdag)
        opplysninger.forDato(førsteUtdanningsdag).finnNullableOpplysning(tarUtdanning)?.verdi shouldBe eksisterendeVerdi
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = [false])
    fun `sjekker senere utdanningsdager selv om den første allerede har true`(senereVerdi: Boolean?) {
        val senereUtdanningsdag = førsteUtdanningsdag.plusDays(2)
        val eksisterende = Faktum(tarUtdanning, true, Gyldighetsperiode(fom, førsteUtdanningsdag))
        val behandling =
            opprettBehandling(
                Opplysninger.med(
                    listOfNotNull(
                        eksisterende,
                        senereVerdi?.let {
                            Faktum(tarUtdanning, it, Gyldighetsperiode(førsteUtdanningsdag.plusDays(1)))
                        },
                    ),
                ),
                utdanningsdager = listOf(førsteUtdanningsdag, senereUtdanningsdag),
            )

        val utdanning =
            behandling.opplysninger.kunEgne
                .finnAlle(tarUtdanning)
                .single()
        utdanning.verdi shouldBe true
        utdanning.gyldighetsperiode shouldBe Gyldighetsperiode(senereUtdanningsdag)
        behandling.opplysninger
            .forDato(førsteUtdanningsdag)
            .finnOpplysning(tarUtdanning)
            .id shouldBe eksisterende.id
    }

    @Test
    fun `legger ikke til tarUtdanning uten utdanning på meldekortet`() {
        val behandling =
            opprettBehandling(
                Opplysninger.med(Faktum(tarUtdanning, false, Gyldighetsperiode(fom))),
                utdanningsdager = emptyList(),
            )

        behandling.opplysninger.kunEgne.finnAlle(tarUtdanning) shouldBe emptyList()
        behandling.opplysninger
            .forDato(fom)
            .finnOpplysning(tarUtdanning)
            .verdi shouldBe false
    }

    private fun opprettBehandling(
        opplysninger: Opplysninger,
        utdanningsdager: List<LocalDate> = listOf(førsteUtdanningsdag),
    ): Behandling {
        val opprettet = tom.plusDays(1).atStartOfDay()
        val meldekort =
            Meldekort(
                id = UUIDv7.ny(),
                meldingsreferanseId = UUIDv7.ny(),
                ident = "12345678910",
                eksternMeldekortId = MeldekortId("meldekort-utdanning"),
                fom = fom,
                tom = tom,
                kilde = MeldekortKilde("Bruker", "12345678910"),
                dager =
                    (0L..13L).map { dag ->
                        val dato = fom.plusDays(dag)
                        Dag(
                            dato = dato,
                            meldt = true,
                            aktiviteter =
                                if (dato in utdanningsdager) {
                                    listOf(MeldekortAktivitet(AktivitetType.Utdanning, null))
                                } else {
                                    emptyList()
                                },
                        )
                    },
                innsendtTidspunkt = opprettet,
                korrigeringAv = null,
                meldedato = opprettet.toLocalDate(),
                kanSendesFra = tom,
            )
        val hendelse =
            BeregnMeldekortHendelse(
                meldingsreferanseId = UUIDv7.ny(),
                ident = meldekort.ident,
                opprettet = opprettet,
                meldekort = meldekort,
            )
        val forrigeBehandling =
            Behandling.rehydrer(
                behandlingId = UUIDv7.ny(),
                behandler = hendelse,
                gjeldendeOpplysninger = opplysninger,
                opprettet = fom.atStartOfDay(),
                tilstand = Behandling.TilstandType.Ferdig,
                sistEndretTilstand = fom.atStartOfDay(),
                avklaringer = emptyList(),
            )

        return (hendelse.behandling(forrigeBehandling, TemporalCollection()) as Opprettet).behandling
    }
}
