package no.nav.dagpenger.modell

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.modell.hendelser.ManuellId
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PersonBehandlingskjedeRegelverkTest {
    private val identStr = "12345678901"
    private val ident = Ident(identStr)

    private val regelverkDagpenger =
        Regelverk(
            navn = RegelverkType("Dagpenger"),
            rettighetsperiodeberegning = { listOf(Rettighetsperiode(LocalDate.now(), LocalDate.MAX, true, true)) },
        )
    private val regelverkFerietillegg =
        Regelverk(
            navn = RegelverkType("Ferietillegg"),
            rettighetsperiodeberegning = { listOf(Rettighetsperiode(LocalDate.now(), LocalDate.MAX, true, true)) },
        )

    @Test
    fun `ferietillegg-hendelse skal ikke kjedes på dagpenger-kjede`() {
        // Oppsett: Person med en ferdig dagpenger-behandling
        val dagpengerHendelse = testHendelse(regelverkDagpenger)
        val dagpengerBehandling = nyFerdigBehandling(dagpengerHendelse)

        val person = Person(ident, listOf(dagpengerBehandling.somKjede()))

        // Handling: Send en ferietillegg-hendelse
        val ferietilleggHendelse = testHendelse(regelverkFerietillegg)
        person.håndter(ferietilleggHendelse)

        // Verifisering: Ferietillegg-behandlingen skal IKKE baseres på dagpenger-behandlingen
        person.behandlinger().size shouldBe 2
        val ferietilleggBehandling = person.behandlinger().last()
        ferietilleggBehandling.basertPå shouldBe null
    }

    @Test
    fun `dagpenger-hendelse skal kjedes på dagpenger-kjede`() {
        // Oppsett: Person med en ferdig dagpenger-behandling
        val dagpengerHendelse = testHendelse(regelverkDagpenger)
        val dagpengerBehandling = nyFerdigBehandling(dagpengerHendelse)

        val person = Person(ident, listOf(dagpengerBehandling.somKjede()))

        // Handling: Send en ny dagpenger-hendelse
        val nyDagpengerHendelse = testHendelse(regelverkDagpenger)
        person.håndter(nyDagpengerHendelse)

        // Verifisering: Ny behandling skal baseres på den forrige dagpenger-behandlingen
        person.behandlinger().size shouldBe 2
        val nyBehandling = person.behandlinger().last()
        nyBehandling.basertPå shouldBe dagpengerBehandling
    }

    @Test
    fun `ferietillegg og dagpenger kjeder uavhengig av hverandre`() {
        // Oppsett: Person med en ferdig dagpenger-kjede og en ferdig ferietillegg-kjede
        val dagpengerHendelse = testHendelse(regelverkDagpenger)
        val dagpengerBehandling = nyFerdigBehandling(dagpengerHendelse)

        val ferietilleggHendelse = testHendelse(regelverkFerietillegg)
        val ferietilleggBehandling = nyFerdigBehandling(ferietilleggHendelse)

        val person =
            Person(
                ident,
                listOf(
                    dagpengerBehandling.somKjede(),
                    ferietilleggBehandling.somKjede(),
                ),
            )

        // Handling: Send en ny dagpenger-hendelse
        val nyDagpengerHendelse = testHendelse(regelverkDagpenger)
        person.håndter(nyDagpengerHendelse)

        // Verifisering: Nye dagpenger-behandlingen baseres på dagpenger-kjeden, ikke ferietillegg
        person.behandlinger().size shouldBe 3
        val nyBehandling = person.behandlinger().single { it.basertPå != null }
        nyBehandling.basertPå shouldBe dagpengerBehandling
    }

    private fun testHendelse(regelverk: Regelverk): RegelverkTestHendelse =
        RegelverkTestHendelse(
            meldingsreferanseId = UUIDv7.ny(),
            ident = identStr,
            testEksternId = ManuellId(UUIDv7.ny()),
            testSkjedde = LocalDate.now(),
            opprettet = LocalDateTime.now(),
            regelverk = regelverk,
        )

    private fun nyFerdigBehandling(hendelse: StartHendelse): Behandling =
        Behandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            behandler = hendelse,
            gjeldendeOpplysninger = Opplysninger(),
            basertPå = null,
            opprettet = LocalDateTime.now(),
            tilstand = Ferdig,
            sistEndretTilstand = LocalDateTime.now(),
            avklaringer = emptyList(),
        )
}

private class RegelverkTestHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    private val testEksternId: ManuellId,
    private val testSkjedde: LocalDate,
    opprettet: LocalDateTime,
    private val regelverk: Regelverk,
) : StartHendelse(
        meldingsreferanseId,
        ident,
        testEksternId,
        testSkjedde,
        opprettet,
    ) {
    override val forretningsprosess: Forretningsprosess =
        object : Forretningsprosess(regelverk) {
            override fun regelkjøring(opplysninger: Opplysninger) =
                Regelkjøring(
                    testSkjedde,
                    opplysninger,
                )

            override fun virkningsdato(opplysninger: LesbarOpplysninger) = testSkjedde
        }

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat =
        StartHendelseResultat.Opprettet(
            Behandling(
                basertPå = forrigeBehandling,
                behandler = this,
                opplysninger = emptyList(),
            ),
        )
}
