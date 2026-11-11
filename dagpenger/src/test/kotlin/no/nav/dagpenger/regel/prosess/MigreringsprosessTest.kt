package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.hendelser.MigreringId
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Utgangspunkt
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.OpplysningsTyper.AldersgrenseId
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.hendelse.MigrertYtelse
import no.nav.dagpenger.regel.hendelse.Tilleggsopplysning
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MigreringsprosessTest {
    @Test
    fun `etterspør aldersgrense når utgangspunktet er konfigurert for migrering`() {
        val produsent =
            Migreringsprosess(setOf(AldersgrenseId))
                .produsenter(prøvingsdato, grunnlag.forDato(prøvingsdato))
                .entries
                .single { (type, _) -> type.id == AldersgrenseId }
                .value

        (produsent is Ekstern<*>) shouldBe true
    }

    @Test
    fun `beholder utgangspunkt når opplysningen ikke er konfigurert for etterspørring`() {
        val produsent =
            Migreringsprosess(emptySet())
                .produsenter(prøvingsdato, grunnlag.forDato(prøvingsdato))
                .entries
                .single { (type, _) -> type.id == AldersgrenseId }
                .value

        (produsent is Utgangspunkt<*>) shouldBe true
    }

    @Test
    fun `etterspør saldoer uten å aktivere meldekortberegning`() {
        val opplysninger = grunnlag
        opplysninger.har(Beregning.meldeperiode) shouldBe false

        val rapport = Migreringsprosess().regelkjøring(opplysninger).evaluer()

        rapport.mangler.intersect(Migreringsprosess.migreringssaldoer) shouldBe
            setOf(Beregning.gjenståendeDager, Beregning.forbrukt, Beregning.gjenståendeEgenandel)
    }

    @Test
    fun `etterspør ikke mottatte historiske saldoer ved gjentatt regelkjøring`() {
        val opplysninger = grunnlag.also { it.leggTilSaldoer(forbrukt = 0) }
        val prosess = Migreringsprosess()

        repeat(2) {
            val rapport = prosess.regelkjøring(opplysninger).evaluer()

            rapport.mangler.intersect(Migreringsprosess.migreringssaldoer) shouldBe emptySet()
        }
        opplysninger.har(Beregning.sisteForbruksdag) shouldBe false
    }

    @Test
    fun `etterspør siste forbruksdato bare når historisk forbruk er positivt`() {
        val opplysninger = grunnlag.also { it.leggTilSaldoer(forbrukt = 20) }
        val prosess = Migreringsprosess()

        prosess
            .regelkjøring(opplysninger)
            .evaluer()
            .mangler
            .intersect(Migreringsprosess.migreringssaldoer) shouldBe
            setOf(Beregning.sisteForbruksdag)

        opplysninger.leggTil(Faktum(Beregning.sisteForbruksdag, prøvingsdato.minusDays(10), saldoperiode))

        prosess
            .regelkjøring(opplysninger)
            .evaluer()
            .mangler
            .intersect(Migreringsprosess.migreringssaldoer) shouldBe emptySet()
    }

    @Test
    fun `etterspør saldo på nytt når den historiske perioden slutter for tidlig`() {
        val opplysninger = grunnlag.also { it.leggTilSaldoer(forbrukt = 0) }
        opplysninger.endre(
            opplysninger.finnOpplysning(Beregning.gjenståendeDager),
            gyldighetsperiode = Gyldighetsperiode.kun(prøvingsdato.minusDays(2)),
        )

        Migreringsprosess()
            .regelkjøring(opplysninger)
            .evaluer()
            .mangler
            .intersect(Migreringsprosess.migreringssaldoer) shouldBe
            setOf(Beregning.gjenståendeDager)
    }

    @Test
    fun `etterspør tilleggssaldoer bare for positive kvotevurderinger`() {
        val kvoter =
            listOf(
                Permittering.oppfyllerKravetTilPermittering to PermitteringFastsetting.regelsett.kvoter.single(),
                Sanksjonsperiode.harSanksjon to Sanksjonsperiode.regelsett.kvoter.single(),
                TidsbegrensetBortfall.harTidsbegrensetBortfall to TidsbegrensetBortfall.regelsett.kvoter.single(),
            )
        kvoter.forEach { (betingelse, kvote) ->
            listOf(null, false, true).forEach { aktiv ->
                val opplysninger = grunnlag.also { it.leggTilSaldoer(forbrukt = 0) }
                opplysninger.leggTil(Faktum(Rettighetstype.skalPermitteringVurderes, aktiv == true))
                if (aktiv != null) opplysninger.leggTil(Faktum(betingelse, aktiv))

                val rapport = Migreringsprosess().regelkjøring(opplysninger).evaluer()
                val kvotesaldoer = setOf(kvote.forbruksteller, kvote.gjenstående, kvote.sisteForbruk)
                rapport.mangler.intersect(kvotesaldoer) shouldBe
                    if (aktiv == true) setOf(kvote.forbruksteller, kvote.gjenstående) else emptySet()

                if (aktiv == true) {
                    opplysninger.leggTil(Faktum(kvote.forbruksteller, 1, saldoperiode))
                    opplysninger.leggTil(Faktum(kvote.gjenstående, 9, saldoperiode))
                    val prosess = Migreringsprosess()
                    prosess
                        .regelkjøring(opplysninger)
                        .evaluer()
                        .mangler
                        .intersect(kvotesaldoer) shouldBe
                        setOf(kvote.sisteForbruk)

                    opplysninger.leggTil(Faktum(kvote.sisteForbruk, prøvingsdato.minusDays(2), saldoperiode))

                    prosess
                        .regelkjøring(opplysninger)
                        .evaluer()
                        .mangler
                        .intersect(kvotesaldoer) shouldBe emptySet()
                }
            }
        }
    }

    private fun Opplysninger.leggTilSaldoer(forbrukt: Int) {
        leggTil(Faktum(Beregning.gjenståendeDager, 260 - forbrukt, saldoperiode))
        leggTil(Faktum(Beregning.forbrukt, forbrukt, saldoperiode))
        leggTil(Faktum(Beregning.gjenståendeEgenandel, Beløp(500), saldoperiode))
    }

    private companion object {
        private val prøvingsdato = LocalDate.of(2026, 1, 5)
        private val saldoperiode = Gyldighetsperiode.kun(prøvingsdato.minusDays(1))

        private val grunnlag
            get() =
                MigrerYtelseHendelse(
                    meldingsreferanseId = UUID.randomUUID(),
                    ident = "12345678910",
                    eksternId = MigreringId(UUID.randomUUID()),
                    opprettet = LocalDateTime.now(),
                    ytelse = MigrertYtelse(migreringTidspunkt = prøvingsdato, innvilgetTilOgMed = null, sakId = 13699879),
                    tilleggsopplysninger = listOf(Tilleggsopplysning(Alderskrav.fødselsdato, LocalDate.of(1990, 1, 1))),
                ).lagFaktagrunnlag()
    }
}
