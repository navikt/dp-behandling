package no.nav.dagpenger.mediator.repository

import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import no.nav.dagpenger.mediator.TestOpplysningstyper.heltall
import no.nav.dagpenger.mediator.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.mediator.db.withMigratedDb
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Utledning
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class OpplysningerRepositoryTracingTest {
    @Test
    fun `måler radmapping og bevarer delte instanser når avhengighetene kommer først`() = verifiserMapping(omvendt = false)

    @Test
    fun `måler radmapping og bevarer delte instanser når avhengighetene kommer sist`() = verifiserMapping(omvendt = true)

    @Test
    fun `registrerer ikke mappingtid for tomt resultat`() {
        withMigratedDb {
            val repo = opplysningerRepository(dbSession)
            val span = spyk(Span.getInvalid())
            every { span.isRecording } returns true

            Context.current().with(span).makeCurrent().use {
                repo.hentOpplysninger(UUIDv7.ny()).rå() shouldBe emptyList()
            }

            verify(exactly = 0) { span.setAttribute("app.row_mapping.duration_ms", any<Double>()) }
        }
    }

    private fun verifiserMapping(omvendt: Boolean) {
        withMigratedDb {
            val repo = opplysningerRepository(dbSession)
            val kilde = Saksbehandlerkilde(UUIDv7.ny(), Saksbehandler("syntetisk"))
            val opprettet = LocalDateTime.of(2025, 1, 1, 12, 0)
            val base =
                Faktum(
                    id = UUID(0, if (omvendt) 3 else 1),
                    opplysningstype = heltall,
                    verdi = 1,
                    kilde = kilde,
                    opprettet = opprettet,
                )
            repo.lagreOpplysninger(Opplysninger.med(base))
            val mellom =
                Faktum(
                    id = UUID(0, 2),
                    opplysningstype = heltall,
                    verdi = 2,
                    utledetAv = Utledning("syntetisk", listOf(base)),
                    kilde = kilde,
                    opprettet = opprettet,
                )
            val siste =
                Faktum(
                    id = UUID(0, if (omvendt) 1 else 3),
                    opplysningstype = heltall,
                    verdi = 3,
                    utledetAv = Utledning("syntetisk", listOf(mellom, base)),
                    kilde = kilde,
                    opprettet = opprettet,
                )
            val opplysninger = Opplysninger.med(mellom, siste)
            repo.lagreOpplysninger(opplysninger)
            siste.erstatter(mellom)
            repo.lagreOpplysninger(opplysninger)
            val span = spyk(Span.getInvalid())
            every { span.isRecording } returns true

            val hentet =
                Context.current().with(span).makeCurrent().use {
                    Span.current() shouldBeSameInstanceAs span
                    repo.hentOpplysninger(opplysninger.id).rå()
                }

            hentet.map { it.id } shouldBe listOf(mellom.id, siste.id).sorted()
            val hentetSiste = hentet.single { it.id == siste.id }
            val hentetMellom = hentet.single { it.id == mellom.id }
            val hentetBase = hentetMellom.utledetAv!!.opplysninger.single()
            hentetSiste.utledetAv!!.opplysninger[0] shouldBeSameInstanceAs hentetMellom
            hentetSiste.utledetAv!!.opplysninger[1] shouldBeSameInstanceAs hentetBase
            hentetSiste.erstatter shouldBeSameInstanceAs hentetMellom
            hentetSiste.kilde shouldBeSameInstanceAs hentetMellom.kilde
            hentetSiste.kilde shouldBeSameInstanceAs hentetBase.kilde
            val mappingTid = slot<Double>()
            verify(exactly = 1) { span.setAttribute("app.row_mapping.duration_ms", capture(mappingTid)) }
            mappingTid.captured shouldBeGreaterThanOrEqual 0.0
        }
    }
}
