package no.nav.dagpenger.behandling.modell

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.hjelpere.juli
import no.nav.dagpenger.behandling.hjelpere.juni
import no.nav.dagpenger.behandling.hjelpere.mai
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.behandling.modell.hendelser.EksternId
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val testIdent = "12312312311"

class PersonTest {
    @Test
    fun `rettighetsperioder og sånt`() {
        val person = Person(Ident(testIdent))
        person.harRettighet(1.juni) shouldBe false

        // Innvilg ny periode
        person.ferdig(
            BehandlingFerdig(
                behandlingResultat(
                    1.juni,
                    Rettighetsperiode(1.juni, 10.juni, true),
                ),
            ),
        )

        person.harRettighet(1.juni) shouldBe true
        person.harRettighet(10.juni) shouldBe true
        person.harRettighet(11.juni) shouldBe false

        // En slags stans
        person.ferdig(
            BehandlingFerdig(
                behandlingResultat(
                    11.juni,
                    Rettighetsperiode(1.juni, 10.juni, true),
                    Rettighetsperiode(11.juni, LocalDate.MAX, false),
                ),
            ),
        )

        person.harRettighet(1.mai) shouldBe false
        person.harRettighet(1.juni) shouldBe true
        person.harRettighet(10.juni) shouldBe true
        person.harRettighet(11.juni) shouldBe false

        // Et slags gjenopptak
        person.ferdig(
            BehandlingFerdig(
                behandlingResultat(
                    11.juni,
                    Rettighetsperiode(1.juni, 10.juni, true),
                    Rettighetsperiode(11.juni, 15.juni, false),
                    Rettighetsperiode(1.juli, 10.juli, true),
                    Rettighetsperiode(11.juli, LocalDate.MAX, false),
                ),
            ),
        )

        person.harRettighet(1.mai) shouldBe false
        person.harRettighet(1.juni) shouldBe true
        person.harRettighet(10.juni) shouldBe true
        person.harRettighet(11.juni) shouldBe false
        person.harRettighet(16.juni) shouldBe false
        person.harRettighet(1.juli) shouldBe true
        person.harRettighet(10.juli) shouldBe true
        person.harRettighet(11.juli) shouldBe false
    }

    @Test
    fun `rettighetsperioder ved avslag`() {
        val person = Person(Ident(testIdent))
        person.harRettighet(1.juni) shouldBe false

        // Innvilg ny periode
        person.ferdig(
            BehandlingFerdig(
                behandlingResultat(
                    1.juni,
                    Rettighetsperiode(1.mai, 31.mai, false),
                    Rettighetsperiode(1.juni, LocalDate.MAX, false),
                ),
            ),
        )

        person.harRettighet(1.juni) shouldBe false
        person.rettighethistorikk().shouldBeEmpty()
    }

    @Test
    fun `rettighetsperioder vsdklgjadfgjiasjdfgkled avslag`() {
        val person = Person(Ident(testIdent))
        person.harRettighet(1.juni) shouldBe false

        // Innvilg ny periode
        person.ferdig(
            BehandlingFerdig(
                behandlingResultat(
                    1.juni,
                    Rettighetsperiode(1.mai, 31.mai, false),
                    Rettighetsperiode(1.juni, LocalDate.MAX, true),
                ),
            ),
        )

        val statuser = person.rettighetstatus
        val mp = Periode(28.mai, 10.juni)
        val potensielleDager =
            mp.associateWith { dag ->
                runCatching { statuser.get(dag).utfall }.getOrElse { false }
            }

        println(potensielleDager)
    }

    private fun behandlingResultat(
        virkningsdato: LocalDate,
        vararg rettighetsperiode: Rettighetsperiode,
    ): Behandling.Resultat =
        Behandling.Resultat(
            behandlingId = UUIDv7.ny(),
            basertPåBehandling = null,
            rettighetsperioder = rettighetsperiode.toList(),
            virkningsdato = virkningsdato,
            behandlingAv = TestHendelse(),
            opplysninger = Opplysninger(),
            automatiskBehandlet = true,
            godkjentAv = Arbeidssteg(Arbeidssteg.Oppgave.Godkjent),
            besluttetAv = Arbeidssteg(Arbeidssteg.Oppgave.Besluttet),
        )

    private class TestHendelse(
        meldingsreferanseId: UUID,
        ident: String,
        eksternId: EksternId<*>,
        skjedde: LocalDate,
        opprettet: LocalDateTime,
        override val forretningsprosess: Forretningsprosess,
    ) : StartHendelse(
            meldingsreferanseId,
            ident,
            eksternId,
            skjedde,
            opprettet,
        ) {
        constructor() : this(
            UUID.randomUUID(),
            testIdent,
            ManuellId(UUIDv7.ny()),
            LocalDate.now(),
            LocalDateTime.now(),
            Testprosess(Regelverk()),
        )

        override fun behandling(
            forrigeBehandling: Behandling?,
            rettighetstatus: TemporalCollection<Rettighetstatus>,
        ): Behandling {
            TODO("Not yet implemented")
        }
    }

    private class Testprosess(
        regelverk: Regelverk,
    ) : Forretningsprosess(regelverk) {
        override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
            TODO("Not yet implemented")
        }

        override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
            TODO("Not yet implemented")
        }

        override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> {
            TODO("Not yet implemented")
        }
    }
}
