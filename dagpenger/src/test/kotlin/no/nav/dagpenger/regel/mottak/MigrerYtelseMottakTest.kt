package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.spun.util.persistence.Loader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.modell.hendelser.MigreringId
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.RegelkjøringLoopException
import no.nav.dagpenger.opplysning.Regelkjøringsrapport
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.regel.DagpengerRegistrering
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.hendelse.MigrertYtelse
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.hendelse.Tilleggsopplysning
import no.nav.dagpenger.regel.prosess.Migreringsprosess
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.beregning.BeregningsperiodeFabrikk
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.Egenandel
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utestengning
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.uuid.UUIDv7
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.dagpenger.inntekt.v1.Inntekt as InntektV1

class MigrerYtelseMottakTest {
    private val jsonMapper = JsonMapper.builder().build()
    private val rapid = TestRapid()
    private val migreringId = UUIDv7.ny()
    private val migreringTidspunkt = LocalDate.of(2026, 1, 10)
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)
    private val opplysningstyper = DagpengerRegistrering().opplysningstyper

    init {
        MigrerYtelseMottak(rapid, hendelseMottaker, opplysningstyper)
    }

    @Test
    fun `bevarer innsendte fakta og gyldighetsperioder i melding med manglende opplysninger`() {
        val fødselsdato = Alderskrav.fødselsdato
        val antallBarn = DagpengenesStørrelse.antallBarn
        val opplysninger =
            listOf(
                opplysning(fødselsdato, "1990-01-01", "2024-01-01"),
                opplysning(antallBarn, 0, "2024-01-01", "2024-06-30"),
                opplysning(antallBarn, 1, "2024-07-01"),
            )
        val sendtMelding = melding(opplysninger)
        rapid.sendTestMessage(sendtMelding)

        val feilet = rapid.inspektør.message(0)
        feilet["@event_name"].asString() shouldBe "migrering_startet"
        feilet["opplysninger"] shouldBe jsonMapper.readTree(sendtMelding)["opplysninger"]
        feilet["manglendeOpplysninger"].values().map { it["uuid"].asString() } shouldBe
            feilet["manglendeOpplysninger"]
                .values()
                .map { it["uuid"].asString() }
                .distinct()
                .sorted()
        feilet["manglendeOpplysninger"].values().forEach { UUID.fromString(it["uuid"].asString()) }
        rapid.inspektør.size shouldBe 1
        rapid.sendTestMessage(feilet.toString())
        rapid.inspektør.size shouldBe 1
        verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
    }

    @Test
    fun `avviser innsendte vilkårsvurderinger`() {
        val vilkår = Opphold.oppfyllerKravet

        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(melding(listOf(opplysning(vilkår, true))))
        }

        verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
    }

    @Test
    fun `frode`() {
        val oppgitt =
            listOf(
                Tilleggsopplysning(
                    Alderskrav.fødselsdato,
                    LocalDate.of(1950, 1, 1),
                    Gyldighetsperiode(LocalDate.of(2024, 1, 1)),
                ),
            )

        val event = hendelse(oppgitt)
        val forhåndskjøring = event.lagFaktagrunnlag()

        val regelkjøringsrapport = event.forretningsprosess.regelkjøring(forhåndskjøring).evaluer()

        regelkjøringsrapport.mangler.filterNot { it in Migreringsprosess.migreringssaldoer }.size shouldBe 1
    }

    @Test
    fun `forhåndskjøring og behandling bygger separate fakta fra samme grunnlag`() {
        val oppgitt =
            Tilleggsopplysning(
                Dagpengegrunnlag.grunnlag,
                Beløp(310630),
                Gyldighetsperiode(LocalDate.of(2024, 1, 1)),
            )
        val event = hendelse(listOf(oppgitt))
        val forhåndskjøring = event.lagFaktagrunnlag()

        event.forretningsprosess.regelkjøring(forhåndskjøring).evaluer()

        val behandling =
            event
                .behandling(null, TemporalCollection())
                .let { it as StartHendelseResultat.Opprettet }
                .behandling
        val behandlingsopplysninger = behandling.opplysninger()

        behandlingsopplysninger.finnOpplysning(Dagpengegrunnlag.grunnlag).verdi shouldBe Beløp(310630)
        behandlingsopplysninger.har(DagpengenesStørrelse.antallBarn) shouldBe false
        behandlingsopplysninger.har(Opphold.oppfyllerKravet) shouldBe false
        forhåndskjøring.finnOpplysning(Dagpengegrunnlag.grunnlag).id shouldNotBe
            behandlingsopplysninger.finnOpplysning(Dagpengegrunnlag.grunnlag).id
        val søknadId = forhåndskjøring.finnOpplysning(Søknadstidspunkt.søknadIdOpplysningstype).verdi
        UUID.fromString(søknadId).version() shouldBe 4
        behandlingsopplysninger.finnOpplysning(Søknadstidspunkt.søknadIdOpplysningstype).verdi shouldBe søknadId
        event.lagFaktagrunnlag().finnOpplysning(Søknadstidspunkt.søknadIdOpplysningstype).verdi shouldBe søknadId
        hendelse().lagFaktagrunnlag().finnOpplysning(Søknadstidspunkt.søknadIdOpplysningstype).verdi shouldNotBe søknadId
    }

    @Test
    fun `overstyrer standardverdier og legger til andre fakta før regelkjøringen`() {
        val resultat =
            lagFerdigFaktagrunnlag(
                fødselsdato = LocalDate.of(1950, 1, 1),
                overstyringer =
                    listOf(
                        Tilleggsopplysning(Rettighetstype.ordinærArbeid, false),
                        Tilleggsopplysning(ReellArbeidssøker.ønsketArbeidstid, 20.0),
                        Tilleggsopplysning(ReellArbeidssøker.erArbeidsfør, false),
                        Tilleggsopplysning(Dagpengegrunnlag.grunnlag, Beløp(310630)),
                    ),
            )

        resultat.rapport.erFerdig() shouldBe true
        resultat.forhåndskjøring.finnOpplysning(Rettighetstype.ordinærArbeid).verdi shouldBe false
        resultat.forhåndskjøring.finnOpplysning(ReellArbeidssøker.ønsketArbeidstid).verdi shouldBe 20.0
        resultat.forhåndskjøring.finnOpplysning(ReellArbeidssøker.erArbeidsfør).verdi shouldBe false
        resultat.forhåndskjøring.finnOpplysning(ReellArbeidssøker.kanJobbeDeltid).verdi shouldBe true
        resultat.forhåndskjøring.finnOpplysning(Dagpengegrunnlag.grunnlag).verdi shouldBe Beløp(310630)
        resultat.forhåndskjøring.finnOpplysning(Rettighetstype.lønnsgaranti).verdi shouldBe false
    }

    @Test
    fun `standardfakta beskriver ordinær ytelse med bosted i Norge og reell arbeidssøker`() {
        val opplysninger = lagFerdigFaktagrunnlag(LocalDate.of(1950, 1, 1)).forhåndskjøring

        opplysninger.finnOpplysning(Søknadstidspunkt.ønsketdato).verdi shouldBe
            opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi
        listOf(
            Opphold.bostedsland,
            ReellArbeidssøker.kanJobbeDeltid,
            ReellArbeidssøker.kanJobbeHvorSomHelst,
            ReellArbeidssøker.erArbeidsfør,
            ReellArbeidssøker.villigTilEthvertArbeid,
            Rettighetstype.skalReellArbeidssøkerVurderes,
            Rettighetstype.ordinærArbeid,
        ).forEach { opplysninger.finnOpplysning(it).verdi shouldBe true }
        opplysninger.finnOpplysning(ReellArbeidssøker.ønsketArbeidstid).verdi shouldBe 37.5
        listOf(
            Utdanning.tarUtdanning,
            Verneplikt.avtjentVerneplikt,
            Rettighetstype.skalPermitteringVurderes,
            Rettighetstype.skalPermitteringFiskeforedlingVurderes,
            Rettighetstype.skalVernepliktVurderes,
            Rettighetstype.skalGjenopptakVurderes,
            Rettighetstype.skalEksportVurderes,
            Rettighetstype.skalEtableringVurderes,
            Rettighetstype.lønnsgaranti,
        ).forEach { opplysninger.finnOpplysning(it).verdi shouldBe false }
    }

    @Test
    fun `starter behandling når rapporten er ferdig også når beregnet vilkår er false`() {
        val ferdigFaktagrunnlag = lagFerdigFaktagrunnlag(LocalDate.of(1950, 1, 1))
        ferdigFaktagrunnlag.rapport.erFerdig() shouldBe true
        ferdigFaktagrunnlag.rapport.manglerOpplysninger() shouldBe false
        ferdigFaktagrunnlag.forhåndskjøring.finnOpplysning(Alderskrav.kravTilAlder).verdi shouldBe false
        ferdigFaktagrunnlag
            .forhåndskjøring
            .finnOpplysning(Alderskrav.fødselsdato)
            .kilde
            ?.meldingsreferanseId shouldBe ferdigFaktagrunnlag.event.meldingsreferanseId

        rapid.sendTestMessage(
            melding(
                ferdigFaktagrunnlag.opplysninger.map { (type, verdi) ->
                    opplysning(type, jsonVerdi(verdi))
                },
            ),
        )

        val hendelse = slot<MigrerYtelseHendelse>()
        verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }
        hendelse.captured.eksternId shouldBe MigreringId(migreringId)
        rapid.inspektør.size shouldBe 0

        val behandling =
            hendelse.captured
                .behandling(null, TemporalCollection())
                .let { it as StartHendelseResultat.Opprettet }
                .behandling
        behandling.opplysninger.har(Alderskrav.kravTilAlder) shouldBe false
        behandling.opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi shouldBe LocalDate.of(2026, 1, 5)
        behandling
            .opplysninger
            .finnOpplysning(SøknadInnsendtHendelse.fagsakIdOpplysningstype)
            .verdi shouldBe 13699879
        behandling
            .opplysninger
            .finnOpplysning(Alderskrav.fødselsdato)
            .kilde
            ?.meldingsreferanseId shouldBe hendelse.captured.meldingsreferanseId
        val faktiskRapport =
            hendelse.captured.forretningsprosess
                .regelkjøring(behandling.opplysninger)
                .evaluer()
        faktiskRapport.erFerdig() shouldBe true
        behandling.opplysninger.finnOpplysning(Alderskrav.kravTilAlder).verdi shouldBe false
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    @NullSource
    fun `simulering publiserer simulerteOpplysninger mens false og utelatt felt starter behandling`(simulering: Boolean?) {
        val faktagrunnlag = lagFerdigFaktagrunnlag(LocalDate.of(1990, 1, 1))
        val sendtMelding =
            melding(
                opplysninger =
                    faktagrunnlag.opplysninger.map { (type, verdi) ->
                        opplysning(type, jsonVerdi(verdi))
                    },
                simulering = simulering,
            )

        rapid.sendTestMessage(sendtMelding)

        if (simulering == true) {
            verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
            rapid.inspektør.size shouldBe 1
            val svar = rapid.inspektør.message(0)
            svar["@event_name"].asString() shouldBe "migrering_startet"
            svar["simulering"].asBoolean() shouldBe true
            svar["opplysninger"] shouldBe jsonMapper.readTree(sendtMelding)["opplysninger"]
            svar["simulerteOpplysninger"].isArray shouldBe true
            svar["simulerteOpplysninger"]
                .values()
                .single {
                    it["uuid"].asString() ==
                        KravPåDagpenger.harLøpendeRett.id.uuid
                            .toString()
                }["verdi"]
                .isBoolean shouldBe true

            rapid.sendTestMessage(svar.toString())
            rapid.inspektør.size shouldBe 1
            verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
        } else {
            val hendelse = slot<MigrerYtelseHendelse>()
            verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }
            hendelse.captured.eksternId shouldBe MigreringId(migreringId)
            rapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `simuleringsfeil publiserer innkommende melding med tomme simulerte opplysninger`() {
        listOf(
            Exception("Feil med sensitiv feilmelding"),
            RegelkjøringLoopException("Feil med sensitiv feilmelding"),
        ).forEach { exception ->
            val rapidMedFeilendeSimulator = TestRapid()
            val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)
            MigrerYtelseMottak(
                rapidMedFeilendeSimulator,
                hendelseMottaker,
                opplysningstyper,
                simulator = { _, _ -> throw exception },
            )
            val sendtMelding = melding(simulering = true)

            rapidMedFeilendeSimulator.sendTestMessage(sendtMelding)

            rapidMedFeilendeSimulator.inspektør.size shouldBe 1
            val svar = rapidMedFeilendeSimulator.inspektør.message(0)
            svar["@event_name"].asString() shouldBe "migrering_startet"
            jsonMapper.readTree(sendtMelding).properties().forEach { (navn, verdi) ->
                svar[navn] shouldBe verdi
            }
            svar["simulerteOpplysninger"].isArray shouldBe true
            svar["simulerteOpplysninger"].size() shouldBe 0
            verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }

            rapidMedFeilendeSimulator.sendTestMessage(svar.toString())
            rapidMedFeilendeSimulator.inspektør.size shouldBe 1
            verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
        }
    }

    @Test
    fun `simulering gir samme opplysninger som behandling inkludert dagsats og rettighetsperioder`() {
        val inntekt =
            Inntekt(
                InntektV1(
                    inntektsId = UUIDv7.ny().toString(),
                    sisteAvsluttendeKalenderMåned = YearMonth.of(2025, 11),
                    inntektsListe =
                        (0L..35L).map { måned ->
                            KlassifisertInntektMåned(
                                årMåned = YearMonth.of(2025, 11).minusMonths(måned),
                                klassifiserteInntekter =
                                    listOf(KlassifisertInntekt(50000.toBigDecimal(), InntektKlasse.ARBEIDSINNTEKT)),
                                harAvvik = false,
                            )
                        },
                ),
            )
        val barn =
            BarnListe(
                barn = listOf(Barn(fødselsdato = LocalDate.of(2020, 3, 1), kvalifiserer = true)),
            )
        val grunnlag =
            lagFerdigFaktagrunnlag(
                LocalDate.of(1990, 1, 1),
                listOf(
                    Tilleggsopplysning(RegistrertArbeidssøker.registrertArbeidssøker, true),
                    Tilleggsopplysning(Minsteinntekt.inntektFraSkatt, inntekt),
                    Tilleggsopplysning(DagpengenesStørrelse.barn, barn),
                ),
            )
        val fakta = grunnlag.opplysninger.map { (type, verdi) -> opplysning(type, jsonVerdi(verdi)) }

        rapid.sendTestMessage(melding(fakta, simulering = true))

        verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
        rapid.inspektør.size shouldBe 1
        val svar = rapid.inspektør.message(0)
        val simulerte = svar["simulerteOpplysninger"].values().toList()
        simulerte.isNotEmpty() shouldBe true
        simulerte.forEach {
            it.has("kilde") shouldBe false
            it.has("utledetAv") shouldBe false
            it.has("erstatter") shouldBe false
        }

        fun simulert(type: Opplysningstype<*>) = simulerte.single { it["uuid"].asString() == type.id.uuid.toString() }
        simulert(Alderskrav.kravTilAlder)["verdi"].asBoolean() shouldBe true
        simulert(KravPåDagpenger.harLøpendeRett)["verdi"].asBoolean() shouldBe true
        simulert(KravPåDagpenger.harLøpendeRett)["gyldigFraOgMed"].asString() shouldBe "2026-01-05"
        jsonMapper.readValue<Map<String, Any?>>(simulert(Minsteinntekt.inntektFraSkatt)["verdi"].toString()) shouldBe
            jsonMapper.readValue<Map<String, Any?>>(jsonMapper.writeValueAsString(jsonVerdi(inntekt)))
        simulert(DagpengenesStørrelse.barn)["verdi"]["barn"][0]["fødselsdato"].asString() shouldBe "2020-03-01"
        simulert(DagpengenesStørrelse.barn)["verdi"]["barn"][0]["forsørgeransvar"].asBoolean() shouldBe true
        (svar.toString().toByteArray(Charsets.UTF_8).size < 1_000_000) shouldBe true

        rapid.sendTestMessage(melding(fakta, simulering = false))

        val startet = slot<MigrerYtelseHendelse>()
        verify(exactly = 1) { hendelseMottaker.behandle(capture(startet), any(), any()) }
        rapid.inspektør.size shouldBe 1
        val behandling =
            (startet.captured.behandling(null, TemporalCollection()) as StartHendelseResultat.Opprettet).behandling
        behandling.håndter(startet.captured)
        RegelverkDagpenger.avgjørelse(behandling.opplysninger) shouldBe Avgjørelse.Innvilgelse
        simulert(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg)["verdi"].decimalValue() shouldBe
            behandling.opplysninger
                .finnOpplysning(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg)
                .verdi.verdien
        val søknadIdUuid =
            Søknadstidspunkt.søknadIdOpplysningstype.id.uuid
                .toString()
        val forventet =
            behandling.opplysninger.simulerteOpplysninger().filterNot { it.uuid == søknadIdUuid }
        val faktiskeVerdier =
            jsonMapper.readValue<List<Map<String, Any?>>>(
                jsonMapper.writeValueAsString(simulerte.filterNot { it["uuid"].asString() == søknadIdUuid }),
            )
        val forventedeVerdier =
            jsonMapper.readValue<List<Map<String, Any?>>>(jsonMapper.writeValueAsString(forventet))
        faktiskeVerdier.size shouldBe forventedeVerdier.size
        forventedeVerdier.forEachIndexed { indeks, forventetOpplysning ->
            withClue("Opplysning $indeks: ${forventetOpplysning["navn"]} (${forventetOpplysning["uuid"]})") {
                val faktiskOpplysning = faktiskeVerdier[indeks]
                if (forventetOpplysning["uuid"] ==
                    DagpengenesStørrelse.barnSomGirTillegg.id.uuid
                        .toString()
                ) {
                    val faktiskeBarn = faktiskOpplysning.getValue("verdi") as Map<*, *>
                    val forventedeBarn = forventetOpplysning.getValue("verdi") as Map<*, *>
                    UUID.fromString(faktiskeBarn["søknadbarnId"] as String)
                    forventedeBarn["søknadbarnId"] shouldBe
                        behandling.opplysninger
                            .finnOpplysning(DagpengenesStørrelse.barn)
                            .id
                            .toString()
                    faktiskOpplysning - "verdi" shouldBe forventetOpplysning - "verdi"
                    faktiskeBarn - "søknadbarnId" shouldBe forventedeBarn - "søknadbarnId"
                } else {
                    faktiskOpplysning shouldBe forventetOpplysning
                }
            }
        }
    }

    @Test
    fun `simulering med manglende fakta svarer med mangler uten simulerte opplysninger`() {
        rapid.sendTestMessage(melding(simulering = true))

        rapid.inspektør.size shouldBe 1
        val svar = rapid.inspektør.message(0)
        (svar["manglendeOpplysninger"].size() > 0) shouldBe true
        svar.has("simulerteOpplysninger") shouldBe false
        svar.has("rapport") shouldBe false
        verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
    }

    @Test
    fun `simulering rekjører regler når plugin ber om det og beholder kjørte regler i rapporten`() {
        val event = lagFerdigFaktagrunnlag(LocalDate.of(1990, 1, 1)).event
        val plugin = mockk<ProsessPlugin>(relaxed = true)
        var runder = 0
        every { plugin.etterRegelkjøring(any()) } answers {
            if (runder++ == 0) firstArg<Prosesskontekst>().beOmRekjøring()
        }
        event.forretningsprosess.registrer(plugin)
        val fakta = event.lagFaktagrunnlag()

        val rapport = event.forretningsprosess.simulerMigrering(fakta)

        rapport.erFerdig() shouldBe true
        rapport.kjørteRegler.isNotEmpty() shouldBe true
        verify(exactly = 2) { plugin.etterRegelkjøring(any()) }
        verify(exactly = 2) { plugin.regelkjøringFerdig(any()) }
        fakta.har(KravPåDagpenger.harLøpendeRett) shouldBe true
    }

    @Test
    fun `simulering returnerer nye mangler etter rekjøring`() {
        val event = lagFerdigFaktagrunnlag(LocalDate.of(1990, 1, 1)).event
        val plugin = mockk<ProsessPlugin>(relaxed = true)
        var runder = 0
        every { plugin.etterRegelkjøring(any()) } answers {
            if (runder++ == 0) {
                val kontekst = firstArg<Prosesskontekst>()
                kontekst.opplysninger.fjern(kontekst.opplysninger.finnOpplysning(Alderskrav.fødselsdato).id)
                kontekst.beOmRekjøring()
            }
        }
        event.forretningsprosess.registrer(plugin)

        val rapport = event.forretningsprosess.simulerMigrering(event.lagFaktagrunnlag())

        rapport.erFerdig() shouldBe false
        (Alderskrav.fødselsdato in rapport.mangler) shouldBe true
        verify(exactly = 1) { plugin.regelkjøringFerdig(any()) }
    }

    @Test
    fun `simulering avbryter når plugin alltid ber om rekjøring`() {
        val event = lagFerdigFaktagrunnlag(LocalDate.of(1990, 1, 1)).event
        val plugin = mockk<ProsessPlugin>(relaxed = true)
        every { plugin.etterRegelkjøring(any()) } answers {
            firstArg<Prosesskontekst>().beOmRekjøring()
        }
        event.forretningsprosess.registrer(plugin)

        shouldThrow<RegelkjøringLoopException> {
            event.forretningsprosess.simulerMigrering(event.lagFaktagrunnlag())
        }

        verify(exactly = 11) { plugin.etterRegelkjøring(any()) }
    }

    @Test
    fun `supplerer kun etterspurte fakta fra tom melding og oppretter behandling med innvilget dagpenger`() {
        val standardverdier =
            (
                standardfakta() +
                    listOf(
                        Tilleggsopplysning(Alderskrav.fødselsdato, LocalDate.of(1990, 1, 1)),
                        Tilleggsopplysning(RegistrertArbeidssøker.registrertArbeidssøker, true),
                    )
            ).associate { it.opplysningstype to it.verdi }
        val typerEtterUuid = opplysningstyper.associateBy { it.id.uuid.toString() }
        val sendteFakta = linkedMapOf<String, Map<String, Any?>>()
        val startetHendelse = slot<MigrerYtelseHendelse>()
        val dialog = Migreringsdialog()
        var feilrunder = 0
        val meldinger = mutableListOf<String>()

        for (runde in 0 until 30) {
            rapid.reset()
            val sendtMelding = melding(sendteFakta.values.toList())
            dialog.sendt(runde + 1, sendtMelding)
            meldinger.add(sendtMelding)
            rapid.sendTestMessage(sendtMelding)

            if (rapid.inspektør.size == 0) {
                verify(exactly = 1) { hendelseMottaker.behandle(capture(startetHendelse), any(), any()) }
                break
            }

            verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
            rapid.inspektør.size shouldBe 1
            val feilet = rapid.inspektør.message(0)
            meldinger.add(feilet.toString())
            dialog.mottatt(runde + 1, feilet)
            feilet["@event_name"].asString() shouldBe "migrering_startet"
            feilet["opplysninger"].values().map { it["uuid"].asString() }.sorted() shouldBe sendteFakta.keys.sorted()
            feilet["opplysninger"] shouldBe jsonMapper.readTree(sendtMelding)["opplysninger"]
            val manglende = feilet["manglendeOpplysninger"].values().map { it["uuid"].asString() }
            manglende.contains(
                Søknadstidspunkt.søknadIdOpplysningstype.id.uuid
                    .toString(),
            ) shouldBe false
            manglende.contains(
                Søknadstidspunkt.søknadsdato.id.uuid
                    .toString(),
            ) shouldBe false
            manglende.isNotEmpty() shouldBe true
            manglende.none { it in sendteFakta } shouldBe true
            feilrunder++

            manglende.forEach { uuid ->
                val type = typerEtterUuid.getValue(uuid)
                val verdi = standardverdier[type] ?: syntetiskVerdi(type)
                sendteFakta[uuid] =
                    if (type in Migreringsprosess.migreringssaldoer) {
                        opplysning(type, jsonVerdi(verdi))
                    } else {
                        opplysning(type, jsonVerdi(verdi), "2026-01-05")
                    }
            }
        }

        startetHendelse.isCaptured shouldBe true
        (feilrunder > 1) shouldBe true
        startetHendelse.captured.eksternId shouldBe MigreringId(migreringId)
        val behandling =
            (startetHendelse.captured.behandling(null, TemporalCollection()) as StartHendelseResultat.Opprettet).behandling
        val opplysninger = behandling.opplysninger
        opplysninger.har(KravPåDagpenger.harLøpendeRett) shouldBe false
        UUID.fromString(opplysninger.finnOpplysning(Søknadstidspunkt.søknadIdOpplysningstype).verdi).version() shouldBe 4

        val prosess = startetHendelse.captured.forretningsprosess
        val rapport = prosess.regelkjøring(opplysninger).evaluer()
        rapport.erFerdig() shouldBe true
        rapport.mangler shouldBe emptyList()
        prosess.kjørRegelkjøringFerdig(Prosesskontekst(opplysninger))

        listOf(
            Alderskrav.kravTilAlder,
            Minsteinntekt.minsteinntekt,
            RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker,
            ReellArbeidssøker.kravTilArbeidssøker,
            KravPåDagpenger.harLøpendeRett,
        ).forEach { opplysninger.finnOpplysning(it).verdi shouldBe true }
        opplysninger.finnOpplysning(Minsteinntekt.inntekt12).verdi shouldBe Beløp(600000)
        opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi shouldBe LocalDate.of(2026, 1, 5)
        opplysninger.finnOpplysning(Søknadstidspunkt.tidligsteVurderingsdato).verdi shouldBe LocalDate.of(2026, 1, 5)
        opplysninger.finnOpplysning(SøknadInnsendtHendelse.fagsakIdOpplysningstype).verdi shouldBe 13699879
        RegelverkDagpenger.avgjørelse(opplysninger) shouldBe Avgjørelse.Innvilgelse
        val opprinneligNamer = Approvals.namerCreater
        try {
            val path =
                Paths
                    .get("../docs")
                    .toAbsolutePath()
                    .normalize()
                    .toString()
            Approvals.namerCreater = Loader { NamerWrapper({ "migrering-meldingsdialog" }, { path }) }
            Approvals.verify(
                dialog.markdown(RegelverkDagpenger.avgjørelse(opplysninger).toString()),
                Options().forFile().withExtension(".md"),
            )
        } finally {
            Approvals.namerCreater = opprinneligNamer
        }
    }

    @Test
    fun `migrerte råfakta gir rett til dagpenger med tilstrekkelig inntekt alder og registrert arbeidssøker`() {
        val sisteInntektsmåned = YearMonth.of(2025, 11)
        val inntekt =
            Inntekt(
                InntektV1(
                    inntektsId = UUIDv7.ny().toString(),
                    sisteAvsluttendeKalenderMåned = sisteInntektsmåned,
                    inntektsListe =
                        (0L..11L).map { måned ->
                            KlassifisertInntektMåned(
                                årMåned = sisteInntektsmåned.minusMonths(måned),
                                klassifiserteInntekter =
                                    listOf(
                                        KlassifisertInntekt(
                                            beløp = 50000.toBigDecimal(),
                                            inntektKlasse = InntektKlasse.ARBEIDSINNTEKT,
                                        ),
                                    ),
                                harAvvik = false,
                            )
                        },
                ),
            )
        val event =
            hendelse(
                listOf(
                    Tilleggsopplysning(Alderskrav.fødselsdato, LocalDate.of(1990, 1, 1)),
                    Tilleggsopplysning(Søknadstidspunkt.ønsketdato, LocalDate.of(2026, 1, 5)),
                    Tilleggsopplysning(Minsteinntekt.inntektFraSkatt, inntekt),
                    Tilleggsopplysning(RegistrertArbeidssøker.registrertArbeidssøker, true),
                    Tilleggsopplysning(ReellArbeidssøker.kanJobbeDeltid, true),
                    Tilleggsopplysning(ReellArbeidssøker.kanJobbeHvorSomHelst, true),
                    Tilleggsopplysning(ReellArbeidssøker.erArbeidsfør, true),
                    Tilleggsopplysning(ReellArbeidssøker.villigTilEthvertArbeid, true),
                    Tilleggsopplysning(ReellArbeidssøker.ønsketArbeidstid, 37.5),
                    Tilleggsopplysning(Rettighetstype.skalPermitteringVurderes, false),
                    Tilleggsopplysning(Rettighetstype.skalPermitteringFiskeforedlingVurderes, false),
                    Tilleggsopplysning(Rettighetstype.ordinærArbeid, true),
                    Tilleggsopplysning(Rettighetstype.lønnsgaranti, false),
                    Tilleggsopplysning(Verneplikt.avtjentVerneplikt, false),
                    Tilleggsopplysning(Opphold.bostedsland, true),
                    Tilleggsopplysning(TapAvArbeidsinntektOgArbeidstid.kravPåLønn, false),
                    Tilleggsopplysning(Utdanning.tarUtdanning, false),
                    Tilleggsopplysning(Utestengning.utestengt, false),
                    Tilleggsopplysning(Samordning.sykepenger, false),
                    Tilleggsopplysning(Samordning.omsorgspenger, false),
                    Tilleggsopplysning(Samordning.svangerskapspenger, false),
                    Tilleggsopplysning(Samordning.foreldrepenger, false),
                    Tilleggsopplysning(Samordning.opplæringspenger, false),
                    Tilleggsopplysning(Samordning.pleiepenger, false),
                    Tilleggsopplysning(Samordning.uføre, false),
                    Tilleggsopplysning(SamordingUtenforFolketrygden.andreYtelser, false),
                    Tilleggsopplysning(DagpengenesStørrelse.barn, BarnListe(barn = emptyList())),
                    Tilleggsopplysning(Sanksjonsperiode.oppgirSelvforskyldtArbeidsløshet, false),
                    Tilleggsopplysning(Rettighetstype.skalEtableringVurderes, false),
                    Tilleggsopplysning(Rettighetstype.skalEksportVurderes, false),
                    Tilleggsopplysning(Rettighetstype.skalReellArbeidssøkerVurderes, true),
                    Tilleggsopplysning(Beregning.gjenståendeDager, 520, Gyldighetsperiode.kun(LocalDate.of(2026, 1, 4))),
                    Tilleggsopplysning(Beregning.forbrukt, 0, Gyldighetsperiode.kun(LocalDate.of(2026, 1, 4))),
                    Tilleggsopplysning(Beregning.gjenståendeEgenandel, Beløp(0), Gyldighetsperiode.kun(LocalDate.of(2026, 1, 4))),
                ),
            )
        val opplysninger = event.lagFaktagrunnlag()
        val beregnedeVilkår =
            listOf(
                Alderskrav.kravTilAlder,
                Minsteinntekt.minsteinntekt,
                RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker,
                ReellArbeidssøker.kravTilArbeidssøker,
                KravPåDagpenger.harLøpendeRett,
            )
        beregnedeVilkår.forEach { opplysninger.har(it) shouldBe false }

        val rapport = event.forretningsprosess.regelkjøring(opplysninger).evaluer()

        rapport.mangler.map { it.navn } shouldBe emptyList()
        rapport.erFerdig() shouldBe true
        rapport.manglerOpplysninger() shouldBe false
        event.forretningsprosess.kjørRegelkjøringFerdig(Prosesskontekst(opplysninger))

        opplysninger.finnOpplysning(Minsteinntekt.inntekt12).verdi shouldBe Beløp(600000)
        beregnedeVilkår.forEach { opplysninger.finnOpplysning(it).verdi shouldBe true }
    }

    @Test
    fun `avviser tilleggsopplysninger fra objektformatet`() {
        val type = DagpengenesStørrelse.antallBarn
        val packet =
            JsonMessage
                .newMessage(
                    "migrering_startet",
                    mapOf("opplysninger" to mapOf(type.behovId to 2)),
                ).apply { interestedIn("opplysninger") }
        shouldThrow<IllegalArgumentException> {
            packet["opplysninger"].tilleggsopplysninger(migreringTidspunkt, opplysningstyper)
        }.message shouldBe "opplysninger må være en liste"
    }

    @Test
    fun `saldoer uten periode gjelder dagen før migrering og brukes i første meldeperiode`() {
        val sendt =
            melding(
                listOf(
                    opplysning(Beregning.gjenståendeDager, 3) - "gyldigFraOgMed" - "gyldigTilOgMed",
                    opplysning(Beregning.forbrukt, 257) - "gyldigFraOgMed" - "gyldigTilOgMed",
                    opplysning(Beregning.gjenståendeEgenandel, 500) - "gyldigFraOgMed" - "gyldigTilOgMed",
                    opplysning(Beregning.sisteForbruksdag, "2026-01-02") - "gyldigFraOgMed" - "gyldigTilOgMed",
                ),
            )
        val packet =
            JsonMessage.newMessage("migrering_startet", jsonMapper.readValue<Map<String, Any>>(sendt)).apply {
                interestedIn("opplysninger", "migreringTidspunkt", "migreringId", "ident", "sakId", "innvilgetTilOgMed", "initiertAv")
            }
        val opplysninger = MigrerYtelseMessage(packet, opplysningstyper).hendelse.lagFaktagrunnlag()
        val start = LocalDate.of(2026, 1, 5)
        val periode = Gyldighetsperiode(start, start.plusDays(13))
        listOf(
            Beregning.gjenståendeDager,
            Beregning.forbrukt,
            Beregning.gjenståendeEgenandel,
            Beregning.sisteForbruksdag,
        ).forEach { type ->
            opplysninger.finnAlle(type).single().gyldighetsperiode shouldBe Gyldighetsperiode.kun(start.minusDays(1))
        }
        opplysninger.leggTil(Faktum(Dagpengeperiode.antallStønadsdager, 260, periode))
        opplysninger.leggTil(Faktum(Egenandel.egenandel, Beløp(3000), periode))
        opplysninger.leggTil(Faktum(KravPåDagpenger.harLøpendeRett, true, periode))
        opplysninger.leggTil(Faktum(Beregning.meldtITide, true, periode))
        opplysninger.leggTil(Faktum(Beregning.arbeidsdag, true, periode))
        opplysninger.leggTil(Faktum(Beregning.arbeidstimer, 0.0, periode))
        opplysninger.leggTil(Faktum(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg, Beløp(1000), periode))
        opplysninger.leggTil(Faktum(Vanligarbeidstid.fastsattVanligArbeidstid, 37.5, periode))
        opplysninger.leggTil(Faktum(TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon, 50.0, periode))

        val resultat = BeregningsperiodeFabrikk(start, periode.tilOgMed, opplysninger, emptyList()).lagBeregningsperiode().resultat

        resultat.forbruksdager.size shouldBe 3
        resultat.forbruktEgenandel shouldBe Beløp(500)
        resultat.gjenståendeEgenandel shouldBe Beløp(0)
        resultat.utbetaling shouldBe Beløp(2500)
    }

    @Test
    fun `bevarer eksplisitt historisk saldoperiode`() {
        val sendt = melding(listOf(opplysning(Beregning.gjenståendeDager, 40, "2025-12-22", "2026-01-04")))
        val packet =
            JsonMessage.newMessage("migrering_startet", jsonMapper.readValue<Map<String, Any>>(sendt)).apply {
                interestedIn("opplysninger")
            }

        val saldo = packet["opplysninger"].tilleggsopplysninger(LocalDate.of(2026, 1, 5), opplysningstyper).single()

        saldo.gyldighetsperiode shouldBe Gyldighetsperiode(LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 4))
    }

    @Test
    fun `avslutter migrering uten svar når en opplysning har omvendt gyldighetsperiode`() {
        listOf(
            opplysning(Verneplikt.avtjentVerneplikt, false, "2026-01-06", "2026-01-05"),
            opplysning(Beregning.gjenståendeDager, 40, "2026-01-05", "2026-01-04"),
        ).forEach { opplysning ->
            rapid.sendTestMessage(melding(listOf(opplysning)))

            rapid.inspektør.size shouldBe 0
            verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
        }
    }

    @Test
    fun `godtar gyldighetsperiode med samme fra- og tildato`() {
        val type = Verneplikt.avtjentVerneplikt
        val node = jsonMapper.valueToTree<JsonNode>(listOf(opplysning(type, false, "2026-01-05", "2026-01-05")))

        val lest = node.tilleggsopplysninger(migreringTidspunkt, opplysningstyper).single()

        lest.gyldighetsperiode shouldBe Gyldighetsperiode.kun(LocalDate.of(2026, 1, 5))
    }

    @Test
    fun `avviser saldo med ugyldig periode eller verdi`() {
        listOf(
            opplysning(Beregning.gjenståendeDager, 40, "2026-01-05", "2026-01-18"),
            opplysning(Beregning.gjenståendeDager, -1),
            opplysning(Beregning.gjenståendeEgenandel, -500),
            opplysning(Beregning.sisteForbruksdag, "2026-01-05"),
        ).forEach { saldo ->
            val packet =
                JsonMessage.newMessage("migrering_startet", mapOf("opplysninger" to listOf(saldo))).apply {
                    interestedIn("opplysninger")
                }

            shouldThrow<IllegalArgumentException> {
                packet["opplysninger"].tilleggsopplysninger(LocalDate.of(2026, 1, 5), opplysningstyper)
            }
        }
    }

    @Test
    fun `identifiserer innsendte opplysninger med UUID uavhengig av navn`() {
        val type = Verneplikt.avtjentVerneplikt
        val oppgitt = opplysning(type, false, "2026-01-05")
        listOf(oppgitt, oppgitt - "navn", oppgitt + ("navn" to "Et annet navn")).forEach { fakta ->
            val node = jsonMapper.valueToTree<JsonNode>(listOf(fakta))

            val lest = node.tilleggsopplysninger(migreringTidspunkt, opplysningstyper).single()

            lest.opplysningstype shouldBe type
            lest.verdi shouldBe false
            lest.gyldighetsperiode shouldBe Gyldighetsperiode(LocalDate.of(2026, 1, 5))
        }
    }

    @Test
    fun `krever opplysningsliste`() {
        listOf("{}", """{"opplysninger":null}""", """{"opplysninger":"feil"}""").forEach { json ->
            shouldThrow<IllegalArgumentException> {
                jsonMapper.readTree(json).path("opplysninger").tilleggsopplysninger(migreringTidspunkt, opplysningstyper)
            }
        }
    }

    @Test
    fun `bruker migreringstidspunkt når gyldig fra og med mangler og bevarer sluttdato`() {
        val type = Verneplikt.avtjentVerneplikt
        val utenPeriode =
            jsonMapper.valueToTree<JsonNode>(
                listOf(
                    mapOf(
                        "uuid" to type.id.uuid.toString(),
                        "verdi" to false,
                    ),
                    opplysning(type, true, tilOgMed = "2026-06-30") - "gyldigFraOgMed",
                ),
            )

        val lest = utenPeriode.tilleggsopplysninger(migreringTidspunkt, opplysningstyper)

        lest.map { it.opplysningstype } shouldBe listOf(type, type)
        lest.map { it.verdi } shouldBe listOf(false, true)
        lest.map { it.gyldighetsperiode } shouldBe
            listOf(
                Gyldighetsperiode(migreringTidspunkt),
                Gyldighetsperiode(migreringTidspunkt, LocalDate.of(2026, 6, 30)),
            )
    }

    @Test
    fun `leser påkrevd barneliste med fødselsdato og forsørgeransvar`() {
        val verdi =
            mapOf(
                "barn" to
                    listOf(
                        mapOf("fødselsdato" to "2020-03-01", "kvalifiserer" to true, "forsørgeransvar" to false),
                    ),
            )
        val node =
            jsonMapper.valueToTree<JsonNode>(
                listOf(opplysning(DagpengenesStørrelse.barn, verdi)),
            )

        val oppgitt = node.tilleggsopplysninger(migreringTidspunkt, opplysningstyper).single()

        oppgitt.opplysningstype shouldBe DagpengenesStørrelse.barn
        oppgitt.verdi shouldBe
            BarnListe(
                barn = listOf(Barn(fødselsdato = LocalDate.of(2020, 3, 1), kvalifiserer = true, forsørgeransvar = false)),
            )
    }

    @Test
    fun `leser inntekt med klassifisering beløp og gyldighetsperiode`() {
        val inntektsId = UUIDv7.ny().toString()
        val verdi =
            jsonMapper.readTree(
                """
                {
                  "inntektsId": "$inntektsId",
                  "sisteAvsluttendeKalenderMåned": "2025-11",
                  "inntektsListe": [{
                    "årMåned": "2025-10",
                    "klassifiserteInntekter": [{
                      "beløp": 50000.25,
                      "inntektKlasse": "ARBEIDSINNTEKT"
                    }],
                    "harAvvik": true
                  }]
                }
                """.trimIndent(),
            )
        val node =
            jsonMapper.valueToTree<JsonNode>(
                listOf(opplysning(Minsteinntekt.inntektFraSkatt, verdi, "2026-01-05")),
            )

        val oppgitt = node.tilleggsopplysninger(migreringTidspunkt, opplysningstyper).single()
        val inntekt = (oppgitt.verdi as Inntekt).verdi

        oppgitt.opplysningstype shouldBe Minsteinntekt.inntektFraSkatt
        oppgitt.gyldighetsperiode shouldBe Gyldighetsperiode(LocalDate.of(2026, 1, 5))
        inntekt.inntektsId shouldBe inntektsId
        inntekt.sisteAvsluttendeKalenderMåned shouldBe YearMonth.of(2025, 11)
        inntekt.manueltRedigert shouldBe false
        inntekt.inntektsListe shouldBe
            listOf(
                KlassifisertInntektMåned(
                    årMåned = YearMonth.of(2025, 10),
                    klassifiserteInntekter = listOf(KlassifisertInntekt("50000.25".toBigDecimal(), InntektKlasse.ARBEIDSINNTEKT)),
                    harAvvik = true,
                ),
            )
        val redigert = (verdi as tools.jackson.databind.node.ObjectNode).put("manueltRedigert", true)
        val redigertNode = jsonMapper.valueToTree<JsonNode>(listOf(opplysning(Minsteinntekt.inntektFraSkatt, redigert)))
        (redigertNode.tilleggsopplysninger(migreringTidspunkt, opplysningstyper).single().verdi as Inntekt).verdi.manueltRedigert shouldBe
            true
    }

    @Test
    fun `avviser opplysning med ugyldig datatype`() {
        val type = DagpengenesStørrelse.antallBarn
        val packet =
            JsonMessage
                .newMessage(
                    "migrering_startet",
                    mapOf("opplysninger" to listOf(opplysning(type, "to"))),
                ).apply { interestedIn("opplysninger") }

        shouldThrow<IllegalArgumentException> {
            packet["opplysninger"].tilleggsopplysninger(migreringTidspunkt, opplysningstyper)
        }
    }

    @Test
    fun `avviser opplysningsliste med ukjent UUID`() {
        val packet =
            JsonMessage
                .newMessage(
                    "migrering_startet",
                    mapOf(
                        "opplysninger" to
                            listOf(
                                mapOf(
                                    "uuid" to UUIDv7.ny().toString(),
                                    "verdi" to true,
                                    "gyldigFraOgMed" to "2024-01-01",
                                ),
                            ),
                    ),
                ).apply { interestedIn("opplysninger") }

        shouldThrow<IllegalArgumentException> {
            packet["opplysninger"].tilleggsopplysninger(migreringTidspunkt, opplysningstyper)
        }
    }

    private fun hendelse(tilleggsopplysninger: List<Tilleggsopplysning<*>> = emptyList()) =
        MigrerYtelseHendelse(
            meldingsreferanseId = UUIDv7.ny(),
            ident = "12345678910",
            eksternId = MigreringId(migreringId),
            opprettet = LocalDateTime.now(),
            ytelse = MigrertYtelse(LocalDate.of(2026, 1, 5), null, 13699879),
            tilleggsopplysninger = tilleggsopplysninger,
        )

    private fun lagFerdigFaktagrunnlag(
        fødselsdato: LocalDate,
        overstyringer: List<Tilleggsopplysning<*>> = emptyList(),
    ): FerdigFaktagrunnlag {
        val opplysninger = linkedMapOf<Opplysningstype<*>, Any>(Alderskrav.fødselsdato to fødselsdato)

        (standardfakta() + overstyringer).forEach { opplysning ->
            opplysninger[opplysning.opplysningstype] = opplysning.verdi
        }

        repeat(30) {
            val event =
                hendelse(
                    opplysninger.map { (type, verdi) -> lagTilleggsopplysning(type, verdi) },
                )
            val faktagrunnlag = event.lagFaktagrunnlag()
            val rapport = event.forretningsprosess.regelkjøring(faktagrunnlag).evaluer()
            if (rapport.erFerdig()) {
                return FerdigFaktagrunnlag(event, opplysninger.toList(), faktagrunnlag, rapport)
            }

            val nyeMangler = rapport.mangler.filterNot { it in opplysninger }
            check(nyeMangler.isNotEmpty()) {
                "Regelkjøringen ber fortsatt om opplysninger som allerede er sendt: ${rapport.mangler.map { it.navn }}"
            }
            nyeMangler.forEach { type -> opplysninger[type] = syntetiskVerdi(type) }
        }

        error("Fant ikke et komplett faktagrunnlag etter 30 produksjonskjøringer")
    }

    private fun standardfakta(): List<Tilleggsopplysning<*>> =
        listOf(
            Tilleggsopplysning(Søknadstidspunkt.ønsketdato, LocalDate.of(2026, 1, 5)),
            Tilleggsopplysning(Opphold.bostedsland, true),
            Tilleggsopplysning(ReellArbeidssøker.kanJobbeDeltid, true),
            Tilleggsopplysning(ReellArbeidssøker.kanJobbeHvorSomHelst, true),
            Tilleggsopplysning(ReellArbeidssøker.erArbeidsfør, true),
            Tilleggsopplysning(ReellArbeidssøker.villigTilEthvertArbeid, true),
            Tilleggsopplysning(ReellArbeidssøker.ønsketArbeidstid, 37.5),
            Tilleggsopplysning(Utdanning.tarUtdanning, false),
            Tilleggsopplysning(Verneplikt.avtjentVerneplikt, false),
            Tilleggsopplysning(Rettighetstype.ordinærArbeid, true),
            Tilleggsopplysning(Rettighetstype.skalReellArbeidssøkerVurderes, true),
            Tilleggsopplysning(Rettighetstype.skalPermitteringVurderes, false),
            Tilleggsopplysning(Rettighetstype.skalPermitteringFiskeforedlingVurderes, false),
            Tilleggsopplysning(Rettighetstype.skalVernepliktVurderes, false),
            Tilleggsopplysning(Rettighetstype.skalEksportVurderes, false),
            Tilleggsopplysning(Rettighetstype.skalEtableringVurderes, false),
            Tilleggsopplysning(Rettighetstype.lønnsgaranti, false),
            Tilleggsopplysning(Beregning.gjenståendeDager, 520),
            Tilleggsopplysning(Beregning.forbrukt, 0),
            Tilleggsopplysning(Beregning.gjenståendeEgenandel, Beløp(0)),
        )

    @Suppress("UNCHECKED_CAST")
    private fun lagTilleggsopplysning(
        type: Opplysningstype<*>,
        verdi: Any,
    ): Tilleggsopplysning<*> =
        Tilleggsopplysning(
            type as Opplysningstype<Any>,
            verdi,
            if (type in Migreringsprosess.migreringssaldoer) Gyldighetsperiode.kun(LocalDate.of(2026, 1, 4)) else null,
        )

    private fun syntetiskVerdi(type: Opplysningstype<*>): Any =
        when (type.datatype) {
            Boolsk -> {
                false
            }

            Dato -> {
                LocalDate.of(2024, 1, 1)
            }

            Desimaltall -> {
                1.0
            }

            Heltall -> {
                1
            }

            Penger -> {
                Beløp(100000)
            }

            Tekst -> {
                "Norge"
            }

            InntektDataType -> {
                Inntekt(
                    InntektV1(
                        inntektsId = UUIDv7.ny().toString(),
                        sisteAvsluttendeKalenderMåned = YearMonth.of(2025, 11),
                        inntektsListe =
                            (0L..11L).map { måned ->
                                KlassifisertInntektMåned(
                                    årMåned = YearMonth.of(2025, 11).minusMonths(måned),
                                    klassifiserteInntekter =
                                        listOf(
                                            KlassifisertInntekt(
                                                beløp = 50000.toBigDecimal(),
                                                inntektKlasse = InntektKlasse.ARBEIDSINNTEKT,
                                            ),
                                        ),
                                    harAvvik = false,
                                )
                            },
                    ),
                )
            }

            BarnDatatype -> {
                BarnListe(barn = emptyList())
            }

            else -> {
                error("Kan ikke lage migreringsfaktum for ${type.navn} (${type.datatype})")
            }
        }

    private fun jsonVerdi(verdi: Any): Any =
        when (verdi) {
            is Beløp -> {
                verdi.verdien
            }

            is LocalDate -> {
                verdi.toString()
            }

            is BarnListe -> {
                mapOf("barn" to verdi.barn)
            }

            is Inntekt -> {
                mapOf(
                    "inntektsId" to verdi.verdi.inntektsId,
                    "sisteAvsluttendeKalenderMåned" to verdi.verdi.sisteAvsluttendeKalenderMåned.toString(),
                    "manueltRedigert" to verdi.verdi.manueltRedigert,
                    "inntektsListe" to
                        verdi.verdi.inntektsListe.map { måned ->
                            mapOf(
                                "årMåned" to måned.årMåned.toString(),
                                "harAvvik" to måned.harAvvik,
                                "klassifiserteInntekter" to
                                    måned.klassifiserteInntekter.map { inntekt ->
                                        mapOf("beløp" to inntekt.beløp, "inntektKlasse" to inntekt.inntektKlasse.name)
                                    },
                            )
                        },
                )
            }

            else -> {
                verdi
            }
        }

    private data class FerdigFaktagrunnlag(
        val event: MigrerYtelseHendelse,
        val opplysninger: List<Pair<Opplysningstype<*>, Any>>,
        val forhåndskjøring: Opplysninger,
        val rapport: Regelkjøringsrapport,
    )

    private fun melding(
        opplysninger: List<Map<String, Any?>> = emptyList(),
        sakId: Int = 13699879,
        migreringTidspunkt: String = "2026-01-05",
        simulering: Boolean? = null,
    ) = jsonMapper.writeValueAsString(
        mapOf(
            "@event_name" to "migrering_startet",
            "sakId" to sakId,
            "migreringId" to migreringId.toString(),
            "ident" to "12345678910",
            "migreringTidspunkt" to migreringTidspunkt,
            "opplysninger" to opplysninger,
        ) + (simulering?.let { mapOf("simulering" to it) } ?: emptyMap()),
    )

    private fun opplysning(
        type: Opplysningstype<*>,
        verdi: Any,
        fraOgMed: String? = null,
        tilOgMed: String? = null,
    ): Map<String, Any?> =
        buildMap {
            put("uuid", type.id.uuid.toString())
            put("navn", type.navn)
            put("verdi", verdi)
            val erSaldo = type in Migreringsprosess.migreringssaldoer
            put("gyldigFraOgMed", fraOgMed ?: if (erSaldo) "2026-01-04" else "2024-01-01")
            (tilOgMed ?: if (erSaldo) "2026-01-04" else null)?.let { put("gyldigTilOgMed", it) }
        }
}
