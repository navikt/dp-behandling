package no.nav.dagpenger.regel.prosess

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.modell.hendelser.MigreringId
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Utgangspunkt
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.OpplysningsTyper.AldersgrenseId
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.hendelse.MigrertYtelse
import no.nav.dagpenger.regel.hendelse.Tilleggsopplysning
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
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

        rapport.mangler.intersect(Migreringsprosess.kvoterViSkalMigrere) shouldBe
            setOf(Beregning.gjenståendeDager, Beregning.forbrukt, Beregning.gjenståendeEgenandel)
    }

    @Test
    fun `etterspør ikke mottatte historiske saldoer ved gjentatt regelkjøring`() {
        val opplysninger = grunnlag.also { it.leggTilSaldoer(forbrukt = 0) }
        val prosess = Migreringsprosess()

        repeat(2) {
            val rapport = prosess.regelkjøring(opplysninger).evaluer()

            rapport.mangler.intersect(Migreringsprosess.kvoterViSkalMigrere) shouldBe emptySet()
        }
        opplysninger.har(Beregning.sisteForbruksdag) shouldBe false
    }

    @Test
    fun `etterspør sanksjonsuker når bruker oppgir selvforskyldt arbeidsløshet`() {
        val opplysninger =
            grunnlag.also {
                it.leggTil(
                    Faktum(
                        Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet,
                        true,
                        Gyldighetsperiode.kun(prøvingsdato),
                    ),
                )
            }

        val rapport = Migreringsprosess().regelkjøring(opplysninger).evaluer()

        (Sanksjonsperiode.antallSanksjonsuker in rapport.mangler) shouldBe true
        (opplysninger.finnNullableOpplysning(Sanksjonsperiode.harSanksjon)?.verdi == true) shouldBe false
    }

    @Test
    fun `etterspør ikke sanksjonsuker når betingelsen mangler eller er usann`() {
        val betingelseUsann =
            grunnlag.also {
                it.leggTil(
                    Faktum(
                        Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet,
                        false,
                        Gyldighetsperiode.kun(prøvingsdato),
                    ),
                )
            }

        listOf(grunnlag, betingelseUsann).forEach { opplysninger ->
            val rapport = Migreringsprosess().regelkjøring(opplysninger).evaluer()

            (Sanksjonsperiode.antallSanksjonsuker in rapport.mangler) shouldBe false
        }
    }

    @Test
    fun `beholder oppgitte sanksjonsuker fremfor standardverdien`() {
        val antallUkerOppgitt = 7
        val opplysninger =
            grunnlag.also {
                it.leggTil(
                    Faktum(
                        Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet,
                        true,
                        Gyldighetsperiode.kun(prøvingsdato),
                    ),
                )
                it.leggTil(
                    Faktum(
                        Sanksjonsperiode.antallSanksjonsuker,
                        antallUkerOppgitt,
                        Gyldighetsperiode.kun(prøvingsdato),
                    ),
                )
            }

        val rapport = Migreringsprosess().regelkjøring(opplysninger).evaluer()

        (Sanksjonsperiode.antallSanksjonsuker in rapport.mangler) shouldBe false
        opplysninger.finnOpplysning(Sanksjonsperiode.antallSanksjonsuker).verdi shouldBe antallUkerOppgitt
    }

    @Test
    fun `etterspør sanksjonsuker når opplysningen endres fra usann til sann og tilbake`() {
        val opplysninger =
            grunnlag.also {
                it.leggTil(
                    Faktum(
                        Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet,
                        false,
                        Gyldighetsperiode.kun(prøvingsdato),
                    ),
                )
            }
        val prosess = Migreringsprosess()

        (Sanksjonsperiode.antallSanksjonsuker in prosess.regelkjøring(opplysninger).evaluer().mangler) shouldBe false

        val oppgittArbeidsløshet =
            opplysninger.finnOpplysning(Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet)
        opplysninger.endre(oppgittArbeidsløshet, verdi = true)

        (Sanksjonsperiode.antallSanksjonsuker in prosess.regelkjøring(opplysninger).evaluer().mangler) shouldBe true
        (opplysninger.finnNullableOpplysning(Sanksjonsperiode.harSanksjon)?.verdi == true) shouldBe false

        opplysninger.endre(opplysninger.finnOpplysning(Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet), verdi = false)

        (Sanksjonsperiode.antallSanksjonsuker in prosess.regelkjøring(opplysninger).evaluer().mangler) shouldBe false
    }

    @Test
    fun `etterspør betingede migreringsopplysninger bare for riktig verdi på prøvingsdato`() {
        val krav =
            listOf(
                Triple(Samordning.sykepenger, true, setOf(Samordning.sykepengerDagsats)),
                Triple(Samordning.pleiepenger, true, setOf(Samordning.pleiepengerDagsats)),
                Triple(Samordning.omsorgspenger, true, setOf(Samordning.omsorgspengerDagsats)),
                Triple(Samordning.opplæringspenger, true, setOf(Samordning.opplæringspengerDagsats)),
                Triple(Samordning.uføre, true, setOf(Samordning.uføreDagsats)),
                Triple(Samordning.foreldrepenger, true, setOf(Samordning.foreldrepengerDagsats)),
                Triple(Samordning.svangerskapspenger, true, setOf(Samordning.svangerskapspengerDagsats)),
                Triple(
                    SamordingUtenforFolketrygden.andreYtelser,
                    true,
                    setOf(
                        SamordingUtenforFolketrygden.pensjonFraOffentligTjenestepensjonsordning,
                        SamordingUtenforFolketrygden.mottarRedusertUførepensjon,
                        SamordingUtenforFolketrygden.vartpenger,
                        SamordingUtenforFolketrygden.ventelønn,
                        SamordingUtenforFolketrygden.etterlønn,
                        SamordingUtenforFolketrygden.garantilottGFF,
                    ),
                ),
                Triple(
                    SamordingUtenforFolketrygden.pensjonFraOffentligTjenestepensjonsordning,
                    true,
                    setOf(SamordingUtenforFolketrygden.pensjonFraOffentligTjenestepensjonsordningBeløp),
                ),
                Triple(
                    SamordingUtenforFolketrygden.mottarRedusertUførepensjon,
                    true,
                    setOf(SamordingUtenforFolketrygden.redusertUførepensjonBeløp),
                ),
                Triple(SamordingUtenforFolketrygden.vartpenger, true, setOf(SamordingUtenforFolketrygden.vartpengerBeløp)),
                Triple(SamordingUtenforFolketrygden.ventelønn, true, setOf(SamordingUtenforFolketrygden.ventelønnBeløp)),
                Triple(SamordingUtenforFolketrygden.etterlønn, true, setOf(SamordingUtenforFolketrygden.etterlønnBeløp)),
                Triple(SamordingUtenforFolketrygden.garantilottGFF, true, setOf(SamordingUtenforFolketrygden.garantilottGFFBeløp)),
                Triple(
                    Utdanning.tarUtdanning,
                    true,
                    setOf(
                        Utdanning.deltakelseIArbeidsmarkedstiltak,
                        Utdanning.opplæringForInnvandrere,
                        Utdanning.grunnskoleopplæring,
                        Utdanning.høyereYrkesfagligUtdanning,
                        Utdanning.høyereUtdanning,
                        Utdanning.deltakelsePåKurs,
                    ),
                ),
                Triple(
                    Rettighetstype.skalEtableringVurderes,
                    true,
                    setOf(
                        Etablering.nyVirksomhet,
                        Etablering.selvforsørget,
                        Etablering.godkjentNæringsfaglig,
                        Etablering.ikkeSelvforskyldtArbeidsledig,
                        Etablering.overFemtiProsentEierandel,
                        Etablering.sluttDato,
                    ),
                ),
                Triple(
                    Rettighetstype.skalPermitteringVurderes,
                    true,
                    setOf(Permittering.godkjentPermitteringsårsak, Permittering.erPermitteringenMidlertidig),
                ),
                Triple(ReellArbeidssøker.kanJobbeDeltid, false, setOf(ReellArbeidssøker.godkjentDeltidssøker)),
                Triple(ReellArbeidssøker.kanJobbeHvorSomHelst, false, setOf(ReellArbeidssøker.godkjentLokalArbeidssøker)),
                Triple(ReellArbeidssøker.erArbeidsfør, false, setOf(ReellArbeidssøker.godkjentArbeidsufør)),
            )

        krav.forEach { (betingelse, ønsketVerdi, forventede) ->
            listOf(null, !ønsketVerdi, ønsketVerdi).forEach { verdi ->
                val opplysninger = grunnlag
                if (verdi != null) opplysninger.leggTil(Faktum(betingelse, verdi, Gyldighetsperiode.kun(prøvingsdato)))
                val prosess = Migreringsprosess()
                val påPrøvingsdato = opplysninger.forDato(prøvingsdato)
                val ønsket = prosess.ønsketResultat(påPrøvingsdato)
                val produsenter = prosess.produsenter(prøvingsdato, påPrøvingsdato)

                if (verdi == ønsketVerdi) {
                    ønsket.intersect(forventede) shouldBe forventede
                    forventede.forEach { (produsenter[it] is Ekstern<*>) shouldBe true }
                } else {
                    ønsket.intersect(forventede) shouldBe emptySet<Opplysningstype<*>>()
                }
            }
        }
    }

    @Test
    fun `betingelse utenfor prøvingsdato gir ikke betinget krav`() {
        val opplysninger =
            grunnlag.also {
                it.leggTil(Faktum(Samordning.sykepenger, true, Gyldighetsperiode.kun(prøvingsdato.minusDays(1))))
                it.leggTil(Faktum(Samordning.sykepenger, false, Gyldighetsperiode.kun(prøvingsdato)))
            }

        val ønsket = Migreringsprosess().ønsketResultat(opplysninger.forDato(prøvingsdato))

        (Samordning.sykepengerDagsats in ønsket) shouldBe false
    }

    @Test
    fun `etterspør siste forbruksdato bare når historisk forbruk er positivt`() {
        val opplysninger = grunnlag.also { it.leggTilSaldoer(forbrukt = 20) }
        val prosess = Migreringsprosess()

        prosess
            .regelkjøring(opplysninger)
            .evaluer()
            .mangler
            .intersect(Migreringsprosess.kvoterViSkalMigrere) shouldBe
            setOf(Beregning.sisteForbruksdag)

        opplysninger.leggTil(Faktum(Beregning.sisteForbruksdag, prøvingsdato.minusDays(10), saldoperiode))

        prosess
            .regelkjøring(opplysninger)
            .evaluer()
            .mangler
            .intersect(Migreringsprosess.kvoterViSkalMigrere) shouldBe emptySet()
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
            .intersect(Migreringsprosess.kvoterViSkalMigrere) shouldBe
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
