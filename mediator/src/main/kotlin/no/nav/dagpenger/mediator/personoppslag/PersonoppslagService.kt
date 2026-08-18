package no.nav.dagpenger.mediator.personoppslag

import no.nav.dagpenger.mediator.api.models.BeregnetDagDTO
import no.nav.dagpenger.mediator.api.models.RettighetsperiodeResponsDTO
import no.nav.dagpenger.mediator.api.models.YtelsestypeDTO
import no.nav.dagpenger.mediator.repository.BehandlingskjedeOpplysninger
import no.nav.dagpenger.mediator.repository.PersonOpplysningerRepository
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import java.time.LocalDate
import java.util.UUID

/**
 * Svarer på personnivå-spørsmål (rettighetsperioder, beregninger) direkte fra
 * dp-behandlings database, uten å gå veien om personaggregatet.
 *
 * Gjenbruker [RegelverkDagpenger] sine beregningsfunksjoner slik at
 * forretningslogikken er den samme som i behandlingsresultat-eventet.
 * Kontrakten er definert i behandling-api.yaml og DTO-ene genereres av Fabrikt.
 */
class PersonoppslagService(
    private val repository: PersonOpplysningerRepository,
) {
    fun hentRettighetsperioder(
        ident: String,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate?,
    ): List<RettighetsperiodeResponsDTO> {
        val ønsketPeriode = Datoperiode(fraOgMed, tilOgMed ?: LocalDate.MAX)
        return repository
            .hentRelevanteOpplysninger(ident)
            .flatMap { kjede -> kjede.tilRettighetsperioder() }
            .filter { Datoperiode(it.fraOgMed, it.tilOgMed ?: LocalDate.MAX) overlapper ønsketPeriode }
            .sortedBy { it.fraOgMed }
    }

    fun hentBeregninger(
        ident: String,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate?,
    ): List<BeregnetDagDTO> {
        val ønsketPeriode = Datoperiode(fraOgMed, tilOgMed ?: LocalDate.MAX)
        return repository
            .hentRelevanteOpplysninger(ident)
            .flatMap { kjede -> kjede.tilBeregnedeDager() }
            .filter { Datoperiode(it.fraOgMed, it.tilOgMed) overlapper ønsketPeriode }
            .sortedBy { it.fraOgMed }
    }

    private fun BehandlingskjedeOpplysninger.tilRettighetsperioder(): List<RettighetsperiodeResponsDTO> {
        val rettighetstyper = opplysninger.rettighetstyper()
        return RegelverkDagpenger
            .rettighetsperioder(opplysninger)
            .map { periode ->
                RettighetsperiodeResponsDTO(
                    fraOgMed = periode.fraOgMed,
                    tilOgMed = periode.tilOgMed.takeIf { it != LocalDate.MAX },
                    harRett = periode.harRett,
                    ytelseType = ytelseTypeFor(rettighetstyper, periode.fraOgMed, periode.tilOgMed),
                )
            }
    }

    private fun BehandlingskjedeOpplysninger.tilBeregnedeDager(): List<BeregnetDagDTO> =
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
     * Finn rettighetstypen som dekker perioden, med Ordinær som standard.
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
            Rettighetstype.LØNNSGARANTI -> throw IllegalArgumentException("Lønngaranti ikke støttet")
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
     * Gjenstående dager for en dato, med fallback til innvilget antall stønadsdager.
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
