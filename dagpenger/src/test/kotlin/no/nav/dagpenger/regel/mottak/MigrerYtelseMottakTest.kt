package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.modell.hendelser.MigreringId
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.modell.hendelser.hendelseTypeOpplysningstype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelkjøringsrapport
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.regel.Behov
import no.nav.dagpenger.regel.DagpengerRegistrering
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.hendelse.MigrertYtelse
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.hendelse.Tilleggsopplysning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utestengning
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.dagpenger.inntekt.v1.Inntekt as InntektV1

class MigrerYtelseMottakTest {
    private val jsonMapper = JsonMapper.builder().build()
    private val rapid = TestRapid()
    private val migreringId = UUIDv7.ny()
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)
    private val opplysningstyper = DagpengerRegistrering().opplysningstyper

    init {
        MigrerYtelseMottak(rapid, hendelseMottaker, opplysningstyper)
    }

    @Test
    fun `rapporterer mangler fra produksjonsregelkjøringen uten å starte behandling`() {
        val event = hendelse()
        val rapport =
            event.forretningsprosess
                .regelkjøring(event.lagFaktagrunnlag())
                .evaluer()
        val forventedeMangler =
            rapport.mangler
                .map { it.id.uuid.toString() }
                .distinct()
                .sorted()

        rapid.sendTestMessage(melding())

        val feilet = rapid.inspektør.message(0)
        feilet["@event_name"].asString() shouldBe "migrering_feilet"
        feilet["sakId"].asInt() shouldBe 13699879
        feilet["arsak"]["kode"].asString() shouldBe "MANGLENDE_OPPLYSNINGER"
        feilet["mottatteOpplysninger"].size() shouldBe 0
        feilet["manglendeOpplysninger"].values().map { it.asString() } shouldBe forventedeMangler
        feilet["arsak"]["detaljer"].values().map { it.asString() } shouldBe forventedeMangler
        feilet["manglendeOpplysninger"].values().map { UUID.fromString(it.asString()) }
        rapid.inspektør.size shouldBe 1
        verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
    }

    @Test
    fun `oppgir bare UUID-er for fakta avsender faktisk sendte`() {
        val fødselsdato = Alderskrav.fødselsdato
        val antallBarn = DagpengenesStørrelse.antallBarn
        val opplysninger =
            listOf(
                opplysning(fødselsdato, "1990-01-01", "2024-01-01"),
                opplysning(antallBarn, 0, "2024-01-01", "2024-06-30"),
                opplysning(antallBarn, 1, "2024-07-01"),
            )
        val mottatteUuid = listOf(fødselsdato.id.uuid.toString(), antallBarn.id.uuid.toString()).sorted()

        rapid.sendTestMessage(melding(opplysninger))

        val feilet = rapid.inspektør.message(0)
        feilet["mottatteOpplysninger"].values().map { it.asString() } shouldBe mottatteUuid
        feilet["manglendeOpplysninger"].values().map { it.asString() } shouldBe
            feilet["manglendeOpplysninger"]
                .values()
                .map { it.asString() }
                .distinct()
                .sorted()
        feilet["manglendeOpplysninger"].values().map { UUID.fromString(it.asString()) }
        feilet.toString().contains("1990-01-01") shouldBe false
        feilet.toString().contains("\"verdi\"") shouldBe false
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
        val forhåndskjørtHendelseType = forhåndskjøring.finnOpplysning(hendelseTypeOpplysningstype)
        val behandletHendelseType = behandlingsopplysninger.finnOpplysning(hendelseTypeOpplysningstype)
        forhåndskjørtHendelseType.verdi shouldBe behandletHendelseType.verdi
        forhåndskjørtHendelseType.kilde?.meldingsreferanseId shouldBe event.meldingsreferanseId
        behandletHendelseType.kilde?.meldingsreferanseId shouldBe event.meldingsreferanseId
        forhåndskjørtHendelseType.id shouldNotBe behandletHendelseType.id
        forhåndskjøring.finnOpplysning(Dagpengegrunnlag.grunnlag).id shouldNotBe
            behandlingsopplysninger.finnOpplysning(Dagpengegrunnlag.grunnlag).id
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
                    Tilleggsopplysning(Søknadstidspunkt.søknadIdOpplysningstype, UUIDv7.ny().toString()),
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
                    lagTilleggsopplysning(opplysningstyper.single { it.behovId == Behov.Ordinær }, true),
                    lagTilleggsopplysning(opplysningstyper.single { it.behovId == Behov.Lønnsgaranti }, false),
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
                    lagTilleggsopplysning(opplysningstyper.single { it.behovId == Behov.Sanksjon }, false),
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
    fun `avbruddshjelperen beholder korrelasjonsfeltene`() {
        val context = mockk<MessageContext>(relaxed = true)
        val publiserte = mutableListOf<String>()

        context.avbrytMigrering(
            123,
            Migreringsavbrudd(kode = "ANNEN_ÅRSAK", melding = "En annen årsak", detaljer = listOf("detalj")),
        )

        verify(exactly = 1) { context.publish(capture(publiserte)) }
        val avbrudd = JsonMapper.builder().build().readTree(publiserte.single())
        avbrudd["@event_name"].asString() shouldBe "migrering_feilet"
        avbrudd["sakId"].asInt() shouldBe 123
        avbrudd["arsak"]["detaljer"].values().map { it.asString() } shouldBe listOf("detalj")
        UUID.fromString(avbrudd["@id"].asString())
        LocalDateTime.parse(avbrudd["@opprettet"].asString())
    }

    @Test
    fun `leser tilleggsopplysninger fra objektformatet`() {
        val type = DagpengenesStørrelse.antallBarn
        val packet =
            JsonMessage
                .newMessage(
                    "migrering_startet",
                    mapOf("opplysninger" to mapOf(type.behovId to 2)),
                ).apply { interestedIn("opplysninger") }
        val opplysninger = packet["opplysninger"].tilleggsopplysninger(opplysningstyper)

        opplysninger.single().opplysningstype shouldBe type
        opplysninger.single().verdi shouldBe 2
    }

    @Test
    fun `avviser opplysning med ugyldig datatype`() {
        val type = DagpengenesStørrelse.antallBarn
        val packet =
            JsonMessage
                .newMessage(
                    "migrering_startet",
                    mapOf("opplysninger" to mapOf(type.behovId to "to")),
                ).apply { interestedIn("opplysninger") }

        shouldThrow<IllegalArgumentException> {
            packet["opplysninger"].tilleggsopplysninger(opplysningstyper)
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
            packet["opplysninger"].tilleggsopplysninger(opplysningstyper)
        }
    }

    @Test
    fun `hopper over den eksplisitt unntatte saken uten å forhåndskjøre`() {
        rapid.sendTestMessage(melding(sakId = 13699878))

        val feilet = rapid.inspektør.message(0)
        feilet["arsak"]["kode"].asString() shouldBe "SKIPPER_MIGRERING"
        rapid.inspektør.size shouldBe 1
        verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
    }

    @Test
    fun `lar tekniske feil fra regelkjøringen forplante seg`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(melding(innvilgetFraOgMed = LocalDate.MAX.toString()))
        }

        rapid.inspektør.size shouldBe 0
        verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
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

    private fun lagFerdigFaktagrunnlag(fødselsdato: LocalDate): FerdigFaktagrunnlag {
        val opplysninger = linkedMapOf<Opplysningstype<*>, Any>(Alderskrav.fødselsdato to fødselsdato)

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

    @Suppress("UNCHECKED_CAST")
    private fun lagTilleggsopplysning(
        type: Opplysningstype<*>,
        verdi: Any,
    ): Tilleggsopplysning<*> = Tilleggsopplysning(type as Opplysningstype<Any>, verdi)

    private fun syntetiskVerdi(type: Opplysningstype<*>): Any =
        when (type.datatype) {
            Boolsk -> false
            Dato -> LocalDate.of(2024, 1, 1)
            Desimaltall -> 1.0
            Heltall -> 1
            Penger -> Beløp(100000)
            Tekst -> "Norge"
            else -> error("Kan ikke lage migreringsfaktum for ${type.navn} (${type.datatype})")
        }

    private fun jsonVerdi(verdi: Any): Any =
        when (verdi) {
            is Beløp -> verdi.verdien
            is LocalDate -> verdi.toString()
            else -> verdi
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
        innvilgetFraOgMed: String = "2026-01-05",
    ) = jsonMapper.writeValueAsString(
        mapOf(
            "@event_name" to "migrering_startet",
            "sakId" to sakId,
            "migreringId" to migreringId.toString(),
            "ident" to "12345678910",
            "innvilgetFraOgMed" to innvilgetFraOgMed,
            "opplysninger" to opplysninger,
        ),
    )

    private fun opplysning(
        type: Opplysningstype<*>,
        verdi: Any,
        fraOgMed: String = "2024-01-01",
        tilOgMed: String? = null,
    ): Map<String, Any?> =
        buildMap {
            put("uuid", type.id.uuid.toString())
            put("verdi", verdi)
            put("gyldigFraOgMed", fraOgMed)
            tilOgMed?.let { put("gyldigTilOgMed", it) }
        }
}
