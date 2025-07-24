package no.nav.dagpenger.behandling.modell

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.date.shouldBeWithin
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.UnderBehandling
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.UnderOpprettelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingTest {
    private val ident = "123456789011"
    private val søknadId = UUIDv7.ny()
    private val testHendelse =
        TestHendelse(
            meldingsreferanseId = søknadId,
            ident = ident,
            søknadId = søknadId,
            gjelderDato = LocalDate.now(),
            fagsakId = 1,
            opprettet = LocalDateTime.now(),
        )

    private companion object {
        val tidligereOpplysning =
            Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "opplysning-fra-tidligere-behandling")
    }

    @Test
    fun `hvilke behandlinger som skal føre til totrinnskontroll`() {
        // innvilgelse krever totrinnskontroll
        kreverTotrinnskontroll(true, true, true) shouldBe true

        // innvilgelse som mangler inntekt krever totrinnskontroll (bug)
        kreverTotrinnskontroll(true, false, true) shouldBe true

        // avslag på inntekt krever ikke totrinnskontroll
        kreverTotrinnskontroll(false, false, true) shouldBe false

        // avslag på inntekt krever ikke totrinnskontroll
        kreverTotrinnskontroll(false, true, true) shouldBe true

        // avslag på alder krever ikke totrinnskontroll
        kreverTotrinnskontroll(false, true, false) shouldBe false

        // avslag på både inntekt og alder krever ikke totrinnskontroll
        kreverTotrinnskontroll(false, false, false) shouldBe false
    }

    fun kreverTotrinnskontroll(
        kravPåDagpenger: Boolean,
        minsteinntekt: Boolean,
        alder: Boolean,
    ) = kravPåDagpenger || (minsteinntekt && alder)

    @Test
    fun `Behandling basert på tidligere behandlinger`() {
        val behandlingskjede = behandlingskjede(5, testHendelse)
        behandlingskjede.opplysninger().somListe() shouldHaveSize 5
        behandlingskjede.opplysninger().somListe().map {
            it.verdi
        } shouldContainAll listOf(1.0, 2.0, 3.0, 4.0, 5.0)
    }

    private fun behandlingskjede(
        antall: Int,
        hendelse: TestHendelse,
    ): Behandling {
        var fomTom = LocalDate.now()
        var forrigeBehandling: Behandling? = null
        for (nummer in 1..antall) {
            val behandling =
                Behandling.rehydrer(
                    behandlingId = UUIDv7.ny(),
                    behandler = hendelse,
                    gjeldendeOpplysninger =
                        Opplysninger.med(
                            Faktum(
                                tidligereOpplysning,
                                nummer.toDouble(),
                                Gyldighetsperiode(fomTom, fomTom),
                            ),
                        ),
                    basertPå = forrigeBehandling,
                    tilstand = Ferdig,
                    sistEndretTilstand = LocalDateTime.now(),
                    avklaringer = emptyList(),
                )
            forrigeBehandling = behandling
            // TODO: Det burde eksplodere uten denne
            //  fomTom = fomTom.plusDays(1)
        }
        return forrigeBehandling!!
    }

    @Test
    fun `behandling varsler om endret tilstand`() {
        val behandling =
            Behandling(
                behandler =
                    testHendelse.also {
                        testHendelse.kontekst(it)
                    },
                opplysninger = emptyList(),
            )

        val observatør = TestObservatør().also { behandling.registrer(it) }
        behandling.håndter(testHendelse)

        observatør.endretTilstandEventer shouldHaveSize 1
        observatør.endretTilstandEventer.first().run {
            forrigeTilstand shouldBe UnderOpprettelse
            gjeldendeTilstand shouldBe UnderBehandling
            forventetFerdig.shouldBeWithin(Duration.ofHours(5), LocalDateTime.now())
        }
    }

    private class TestObservatør : BehandlingObservatør {
        val endretTilstandEventer = mutableListOf<BehandlingObservatør.BehandlingEndretTilstand>()

        override fun endretTilstand(event: BehandlingObservatør.BehandlingEndretTilstand) {
            endretTilstandEventer.add(event)
        }
    }
}

private class TestHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    søknadId: UUID,
    gjelderDato: LocalDate,
    fagsakId: Int,
    opprettet: LocalDateTime,
) : StartHendelse(
        meldingsreferanseId,
        ident,
        SøknadId(søknadId),
        gjelderDato,
        opprettet,
    ) {
    private val opplysningstypeBehov = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "trengerDenne")
    private val opplysningstype = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "opplysning")
    override val forretningsprosess: Forretningsprosess
        get() =
            object : Forretningsprosess {
                override val regelverk: Regelverk
                    get() = TODO("Not yet implemented")

                override fun regelkjøring(opplysninger: Opplysninger) =
                    Regelkjøring(
                        skjedde,
                        opplysninger,
                        regelsett,
                    )

                override fun kontrollpunkter(): List<IKontrollpunkt> = emptyList()

                override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger): Boolean {
                    TODO("Not yet implemented")
                }

                override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
                    TODO("Not yet implemented")
                }

                override fun regelsett() = listOf(regelsett)

                override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> {
                    TODO("Not yet implemented")
                }
            }

    private val regelsett =
        vilkår("test") {
            regel(opplysningstypeBehov) { innhentes }
            regel(opplysningstype) { enAv(opplysningstypeBehov) }
        }

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        TODO("Not yet implemented")
    }
}
