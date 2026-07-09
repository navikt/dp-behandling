package no.nav.dagpenger.mediator.api

import no.nav.dagpenger.mediator.api.models.BehandlingSammendragDTO
import no.nav.dagpenger.mediator.api.models.PeriodeDTO
import no.nav.dagpenger.mediator.api.models.SakDTO
import no.nav.dagpenger.mediator.api.models.SakStatusDTO
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Behandling.TilstandType.Ferdig
import no.nav.dagpenger.modell.Behandlingkjede
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import java.time.LocalDate
import java.util.UUID

/**
 * Bygger en SakDTO direkte fra en fullt hydrert Behandlingkjede. Dette er bevisst enkelt:
 * hele kjeden hentes og hydreres på vanlig måte (samme vei som GET /behandling/v2), uten egne
 * spørringer eller lesemodeller. Om dette blir en ytelsesutfordring for lange kjeder, bør det
 * håndteres som en egen, separat optimalisering.
 */
internal fun tilSakDTO(
    sakId: UUID,
    kjede: Behandlingkjede,
): SakDTO =
    SakDTO(
        sakId = sakId,
        status = kjede.nesteSomKanBaseresPå?.tilSakStatusDTO() ?: SakStatusDTO(harLøpendeRett = false),
        // Nyeste behandling først, i tråd med hvordan behandlingskjeder presenteres i POST /behandling
        behandlinger = kjede.toList().reversed().map { it.tilBehandlingSammendragDTO() },
    )

private fun Behandling.tilBehandlingSammendragDTO(): BehandlingSammendragDTO =
    BehandlingSammendragDTO(
        behandlingId = behandlingId,
        opprettet = opprettet,
        ferdigstilt = sistEndret.takeIf { harTilstand(Ferdig) },
        behandletHendelse = behandler.tilHendelseDTO(),
        førteTil = takeIf { harTilstand(Ferdig) }?.vedtakopplysninger?.avgjørelse?.tilAvgjørelseDTO(),
    )

private fun Behandling.tilSakStatusDTO(): SakStatusDTO {
    val opplysninger = opplysninger()
    val perioder = vedtakopplysninger.rettighetsperioderRådata()
    val iDag = LocalDate.now()
    val gjeldendePeriode =
        perioder.lastOrNull { !it.fraOgMed.isAfter(iDag) && !it.tilOgMed.isBefore(iDag) }

    val sisteMeldeperiode =
        opplysninger
            .finnAlle(Beregning.meldeperiode)
            .maxByOrNull { it.verdi.tilOgMed }
            ?.verdi

    return SakStatusDTO(
        harLøpendeRett = gjeldendePeriode?.harRett ?: false,
        fraOgMed = gjeldendePeriode?.fraOgMed,
        tilOgMed = gjeldendePeriode?.tilOgMed?.tilApiDato(),
        sisteMeldeperiode = sisteMeldeperiode?.let { PeriodeDTO(fraOgMed = it.fraOgMed, tilOgMed = it.tilOgMed) },
        // sisteGjenståendeDager har en åpen gyldighetsperiode (fra siste forbruksdato og fremover),
        // i motsetning til gjenståendeDager som kun er gyldig per forbruksdag
        gjenståendeDager = opplysninger.finnNullableOpplysning(Beregning.sisteGjenståendeDager)?.verdi,
        sistEndret = sistEndret,
    )
}

private fun Behandling.VedtakOpplysninger.rettighetsperioderRådata() =
    behandlingAv.forretningsprosess.regelverk.rettighetsperioder(opplysninger)
