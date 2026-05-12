package no.nav.dagpenger.behandling.mediator

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.GaugeWithCallback
import io.prometheus.metrics.core.metrics.Histogram
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingAvbrutt
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingForslagTilVedtak
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingOpprettet
import no.nav.dagpenger.behandling.modell.PersonObservatør
import no.nav.dagpenger.behandling.modell.hendelser.ArbeidssøkerperiodeId
import no.nav.dagpenger.behandling.modell.hendelser.FerietilleggId
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.OmgjøringId
import no.nav.dagpenger.behandling.modell.hendelser.SamordningId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Regelsett
import javax.sql.DataSource

internal class BehandlingMetrikker : PersonObservatør {
    override fun opprettet(event: BehandlingOpprettet) {
        behandlingOpprettet.labelValues(event.hendelse.eksternId.hendelseType()).inc()
    }

    override fun endretTilstand(event: BehandlingEndretTilstand) {
        tilstandsendring
            .labelValues(event.forrigeTilstand.name, event.gjeldendeTilstand.name)
            .inc()
    }

    override fun forslagTilVedtak(event: BehandlingForslagTilVedtak) {
        forslagTeller.labelValues(event.behandlingAv.eksternId.hendelseType()).inc()
    }

    override fun ferdig(event: BehandlingFerdig) {
        val harRett = event.rettighetsperioder.any { it.harRett }
        val utfall = if (harRett) "innvilget" else "avslag"
        val hendelseType = event.behandlingAv.eksternId.hendelseType()

        behandlingFerdig
            .labelValues(utfall, hendelseType, event.automatiskBehandlet.toString())
            .inc()

        // Vilkårsmetrikker
        val relevanteVilkår: List<Regelsett> = event.relevanteVilkår()
        val opplysningerPåVirkningsdato = event.opplysningerPåVirkningsdato()
        relevanteVilkår
            .flatMap { regelsett -> regelsett.betingelser }
            .forEach { opplysningstype ->
                opplysningerPåVirkningsdato
                    .somListe()
                    .filter { it.opplysningstype == opplysningstype }
                    .filterIsInstance<Opplysning<Boolean>>()
                    .forEach { opplysning ->
                        val status = if (opplysning.verdi) "oppfylt" else "ikke_oppfylt"
                        vilkaarTeller.labelValues(opplysningstype.navn, status).inc()
                    }
            }

        // Antall egne opplysninger
        opplysningerAntall.observe(
            event.opplysninger.kunEgne
                .somListe()
                .size
                .toDouble(),
        )
    }

    override fun avbrutt(event: BehandlingAvbrutt) {
        behandlingAvbrutt
            .labelValues(
                event.hendelse.hendelseType(),
                event.årsak ?: "ukjent",
            ).inc()
    }

    companion object {
        val behandlingOpprettet: Counter =
            Counter
                .builder()
                .name("dp_behandling_opprettet_total")
                .help("Antall behandlinger opprettet")
                .labelNames("hendelse_type")
                .register()

        val tilstandsendring: Counter =
            Counter
                .builder()
                .name("dp_behandling_tilstandsendring_total")
                .help("Antall tilstandsendringer i behandlinger")
                .labelNames("fra_tilstand", "til_tilstand")
                .register()

        val behandlingFerdig: Counter =
            Counter
                .builder()
                .name("dp_behandling_ferdig_total")
                .help("Antall ferdige behandlinger")
                .labelNames("utfall", "hendelse_type", "automatisk")
                .register()

        val behandlingAvbrutt: Counter =
            Counter
                .builder()
                .name("dp_behandling_avbrutt_total")
                .help("Antall avbrutte behandlinger")
                .labelNames("hendelse_type", "aarsak")
                .register()

        val forslagTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_forslag_total")
                .help("Antall forslag til vedtak")
                .labelNames("hendelse_type")
                .register()

        val vilkaarTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_vilkaar_total")
                .help("Vilkårvurderinger ved ferdigstillelse")
                .labelNames("vilkaar", "status")
                .register()

        val opplysningerAntall: Histogram =
            Histogram
                .builder()
                .name("dp_behandling_opplysninger_antall")
                .help("Antall egne opplysninger per behandling ved ferdigstillelse")
                .register()

        val hendelseTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_hendelse_haandtert_total")
                .help("Antall hendelser håndtert")
                .labelNames("hendelse")
                .register()

        val godkjentTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_godkjent_total")
                .help("Antall godkjente behandlinger")
                .register()

        val besluttetTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_besluttet_total")
                .help("Antall besluttede behandlinger")
                .register()

        val sendtTilbakeTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_sendt_tilbake_total")
                .help("Antall behandlinger sendt tilbake")
                .register()

        val startHendelseMottattTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_start_hendelse_mottatt_total")
                .help("Antall StartHendelser mottatt (sammenlign med opprettet_total for å se avvisningsrate)")
                .labelNames("hendelse")
                .register()

        val opplysningSvarTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_opplysning_svar_total")
                .help("Antall opplysningssvar mottatt")
                .register()

        val meldekortKøStørrelse: io.prometheus.metrics.core.metrics.Gauge =
            io.prometheus.metrics.core.metrics.Gauge
                .builder()
                .name("dp_behandling_meldekort_koe_storrelse")
                .help("Antall meldekort i behandlingskø")
                .labelNames("status")
                .register()

        val utbetalingStatusTeller: Counter =
            Counter
                .builder()
                .name("dp_behandling_utbetaling_status_total")
                .help("Antall utbetalingsstatusendringer mottatt")
                .labelNames("status")
                .register()

        val avklaringLevetid: Histogram =
            Histogram
                .builder()
                .name("dp_behandling_avklaring_levetid_sekunder")
                .help("Tid fra avklaring opprettes til den lukkes, i sekunder")
                .labelNames("kode")
                .register()

        fun registrerBehovGauge(dataSource: DataSource) {
            GaugeWithCallback
                .builder()
                .name("dp_behandling_aktive_behov")
                .help("Antall aktive (uløste) behov per behovtype")
                .labelNames("behov", "status")
                .callback { callback ->
                    try {
                        sessionOf(dataSource).use { session ->
                            session.run(
                                queryOf(
                                    // language=PostgreSQL
                                    """
                                    SELECT behov, status, COUNT(*) as antall
                                    FROM behandling_aktive_behov
                                    GROUP BY behov, status
                                    """.trimIndent(),
                                ).map { row ->
                                    callback.call(
                                        row.long("antall").toDouble(),
                                        row.string("behov"),
                                        row.string("status"),
                                    )
                                }.asList,
                            )
                        }
                    } catch (_: Exception) {
                        // Ignorer feil ved scrape — DB kan være midlertidig utilgjengelig
                    }
                }.register()
        }
    }
}

private fun no.nav.dagpenger.behandling.modell.hendelser.EksternId<*>.hendelseType(): String =
    when (this) {
        is SøknadId -> "Søknad"
        is MeldekortId -> "Meldekort"
        is ManuellId -> "Manuell"
        is OmgjøringId -> "Omgjøring"
        is FerietilleggId -> "Ferietillegg"
        is ArbeidssøkerperiodeId -> "Arbeidssøkerperiode"
        is SamordningId -> "Samordning"
    }
