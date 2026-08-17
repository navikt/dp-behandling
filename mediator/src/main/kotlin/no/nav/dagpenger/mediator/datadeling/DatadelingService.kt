package no.nav.dagpenger.mediator.datadeling

import no.nav.dagpenger.mediator.api.models.BeregnetDagDTO
import no.nav.dagpenger.mediator.api.models.DatadelingForesporselDTO
import no.nav.dagpenger.mediator.api.models.DatadelingPeriodeDTO
import no.nav.dagpenger.mediator.api.models.DatadelingPeriodeResponsDTO
import no.nav.dagpenger.mediator.api.models.YtelsestypeDTO
import no.nav.dagpenger.mediator.repository.BehandlingMedOpplysninger
import no.nav.dagpenger.mediator.repository.DatadelingRepository
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import java.time.LocalDate
import java.util.UUID

/**
 * Bygger datadeling-responser (perioder og beregninger) direkte fra
 * dp-behandlings database, uten å gå veien om personaggregatet.
 *
 * Gjenbruker [RegelverkDagpenger] sine beregningsfunksjoner slik at
 * forretningslogikken er den samme som i behandlingsresultat-eventet.
 * Kontrakten er definert i behandling-api.yaml og DTO-ene genereres av Fabrikt.
 */
class DatadelingService(
    private val repository: DatadelingRepository,
) {
    fun hentPerioder(forespørsel: DatadelingForesporselDTO): DatadelingPeriodeResponsDTO {
        val ønsketPeriode = Datoperiode(forespørsel.fraOgMed, forespørsel.tilOgMed ?: LocalDate.MAX)
        val perioder =
            repository
                .hentFerdigeBehandlinger(forespørsel.ident)
                .flatMap { behandling -> behandling.tilPerioder() }
                .filter { Datoperiode(it.fraOgMed, it.tilOgMed ?: LocalDate.MAX) overlapper ønsketPeriode }
                .sortedBy { it.fraOgMed }

        return DatadelingPeriodeResponsDTO(ident = forespørsel.ident, perioder = perioder)
    }

    fun hentBeregninger(forespørsel: DatadelingForesporselDTO): List<BeregnetDagDTO> {
        val ønsketPeriode = Datoperiode(forespørsel.fraOgMed, forespørsel.tilOgMed ?: LocalDate.MAX)
        return repository
            .hentFerdigeBehandlinger(forespørsel.ident)
            .flatMap { behandling -> behandling.tilBeregnedeDager() }
            .filter { Datoperiode(it.fraOgMed, it.tilOgMed) overlapper ønsketPeriode }
            .sortedBy { it.fraOgMed }
    }

    private fun BehandlingMedOpplysninger.tilPerioder(): List<DatadelingPeriodeDTO> {
        val rettighetstyper = opplysninger.rettighetstyper()
        // Returnerer både perioder med og uten rett — filtrering på harRett gjøres av konsumenten (dp-datadeling)
        return RegelverkDagpenger
            .rettighetsperioder(opplysninger)
            .map { periode ->
                DatadelingPeriodeDTO(
                    fraOgMed = periode.fraOgMed,
                    tilOgMed = periode.tilOgMed.takeIf { it != LocalDate.MAX },
                    harRett = periode.harRett,
                    ytelseType = ytelseTypeFor(rettighetstyper, periode.fraOgMed, periode.tilOgMed),
                )
            }
    }

    private fun BehandlingMedOpplysninger.tilBeregnedeDager(): List<BeregnetDagDTO> =
        RegelverkDagpenger.utbetalinger(opplysninger).map { utbetaling ->
            BeregnetDagDTO(
                fraOgMed = utbetaling.dato,
                tilOgMed = utbetaling.dato,
                sats = utbetaling.sats,
                utbetaltBeløp = utbetaling.utbetaling,
                gjenståendeDager = opplysninger.gjenståendeDager(utbetaling.dato),
            )
        }

    /**
     * Samme utledning som dp-datadelings BehandlingResultatV1Tolker:
     * finn rettighetstypen som dekker perioden, med Ordinær som standard.
     */
    private fun ytelseTypeFor(
        rettighetstyper: List<RettighetstypePeriode>,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
    ): YtelsestypeDTO {
        val dekkende =
            rettighetstyper.firstOrNull { type ->
                !type.fraOgMed.isAfter(fraOgMed) && !type.tilOgMed.isBefore(tilOgMed)
            }
        return when (dekkende?.type) {
            null, Rettighetstype.ORDINÆR -> YtelsestypeDTO.DAGPENGER_ARBEIDSSOKER_ORDINAER
            Rettighetstype.PERMITTERING -> YtelsestypeDTO.DAGPENGER_PERMITTERING_ORDINAER
            Rettighetstype.LØNNSGARANTI -> throw IllegalArgumentException("Lønngaranti ikke støttet i datadeling")
            Rettighetstype.FISK -> YtelsestypeDTO.DAGPENGER_PERMITTERING_FISKEINDUSTRI
        }
    }

    private fun LesbarOpplysninger.rettighetstyper(): List<RettighetstypePeriode> =
        somListe()
            .filter { it.verdi == true }
            .mapNotNull { opplysning ->
                val type = rettighetstypePerUuid[opplysning.opplysningstype.id.uuid] ?: return@mapNotNull null
                RettighetstypePeriode(
                    type = type,
                    fraOgMed = opplysning.gyldighetsperiode.fraOgMed,
                    tilOgMed = opplysning.gyldighetsperiode.tilOgMed,
                )
            }

    /**
     * Samme oppslag som dp-datadelings BehandlingResultatV1Tolker, inkludert
     * fallback til innvilget antall stønadsdager.
     */
    private fun LesbarOpplysninger.gjenståendeDager(dato: LocalDate): Int =
        somListe()
            .filter { it.opplysningstype.id.uuid == Beregning.gjenståendeDager.id.uuid }
            .firstOrNull { it.gyldighetsperiode.fraOgMed == dato }
            ?.verdi as? Int
            ?: somListe()
                .firstOrNull { it.opplysningstype.id.uuid == Dagpengeperiode.antallStønadsdager.id.uuid }
                ?.verdi as? Int
            ?: throw IllegalStateException("Finner ikke antall innvilgede dager")

    private data class RettighetstypePeriode(
        val type: Rettighetstype,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )

    private enum class Rettighetstype {
        ORDINÆR,
        PERMITTERING,
        LØNNSGARANTI,
        FISK,
    }

    private data class Datoperiode(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    ) {
        infix fun overlapper(other: Datoperiode): Boolean = fraOgMed <= other.tilOgMed && other.fraOgMed <= tilOgMed
    }

    private companion object {
        private val rettighetstypePerUuid: Map<UUID, Rettighetstype> =
            mapOf(
                OpplysningsTyper.HarRettTilOrdinærId.uuid to Rettighetstype.ORDINÆR,
                OpplysningsTyper.PermittertId.uuid to Rettighetstype.PERMITTERING,
                OpplysningsTyper.LønnsgarantiId.uuid to Rettighetstype.LØNNSGARANTI,
                OpplysningsTyper.PermittertFiskeforedlingId.uuid to Rettighetstype.FISK,
            )
    }
}
