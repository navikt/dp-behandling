package no.nav.dagpenger.behandling.modell

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.behandling.hjelpere.juni
import no.nav.dagpenger.behandling.modell.hendelser.EksternId
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class PppersonTest {
    @Test
    fun `en person begynner uten noe`() {
        val person = Ppperson(Ident("12312312311"))
        person.harRettighet(LocalDate.now()) shouldBe false
        person.rettighetsforhold.shouldBeInstanceOf<IngenRettighetsforhold>()
    }

    @Test
    fun `en person kan få noe`() {
        val person = Ppperson(Ident("12312312311"))

        person.håndter(TestHendelse(LocalDate.now(), 5.days))

        person.harRettighet(LocalDate.now()) shouldBe true
        person.rettighetsforhold.shouldBeInstanceOf<LøpendeRettighetsforhold>()

        person.harRettighet(LocalDate.now().plusWeeks(6)) shouldBe false
    }

    @Test
    fun `kaos testing`() {
        val person = Ppperson(Ident("12312312311"))

        person.håndter(TestHendelse(5.juni, 5.days))

        person.harRettighet(5.juni) shouldBe true
        person.rettighetsforhold.shouldBeInstanceOf<LøpendeRettighetsforhold>()
        person.harRettighet(15.juni) shouldBe false

        person.håndter(TestHendelse(20.juni, 5.days))

        person.harRettighet(20.juni) shouldBe true
        person.rettighetsforhold.shouldBeInstanceOf<LøpendeRettighetsforhold>()
        person.rettighetsforhold.behandling
            ?.behandlingId
            .shouldNotBeNull()
    }

    private class TestHendelse(
        meldingsreferanseId: UUID,
        ident: String,
        eksternId: EksternId<*>,
        skjedde: LocalDate,
        opprettet: LocalDateTime,
        override val forretningsprosess: Forretningsprosess,
        private val gyldighetsperiode: Gyldighetsperiode,
    ) : StartHendelse(meldingsreferanseId, ident, eksternId, skjedde, opprettet) {
        constructor(fraOgMed: LocalDate = LocalDate.now(), lengde: Duration) : this(
            UUID.randomUUID(),
            "12312312311",
            ManuellId(UUID.randomUUID()),
            LocalDate.now(),
            LocalDateTime.now(),
            testprosess,
            Gyldighetsperiode(fraOgMed, fraOgMed.plusDays(lengde.inWholeDays)),
        )

        companion object {
            private val løpendeRett = Opplysningstype.boolsk(Opplysningstype.Id(UUID.randomUUID(), Boolsk), "rettighetsperiode")

            private val testprosess =
                object : Forretningsprosess(Regelverk(løpendeRett)) {
                    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring =
                        Regelkjøring(LocalDate.now(), opplysninger, *this.regelsett().toTypedArray())

                    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = LocalDate.now()

                    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> = emptyList()
                }
        }

        override fun behandling(forrigeBehandling: Behandling?): Behandling =
            Behandling(
                this,
                listOf(
                    Faktum(
                        løpendeRett,
                        true,
                        gyldighetsperiode = gyldighetsperiode,
                    ),
                ),
                forrigeBehandling,
            )
    }
}
