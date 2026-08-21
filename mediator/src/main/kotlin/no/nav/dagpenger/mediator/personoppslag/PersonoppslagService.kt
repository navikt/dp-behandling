package no.nav.dagpenger.mediator.personoppslag

import no.nav.dagpenger.mediator.api.models.BeregnetDagDTO
import no.nav.dagpenger.mediator.api.models.RettighetsperiodeResponsDTO
import no.nav.dagpenger.mediator.api.models.YtelsestypeDTO
import no.nav.dagpenger.mediator.repository.KjedeOpplysningerRepository
import no.nav.dagpenger.modell.BehandlingskjedeOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.erPermittert
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.lønnsgaranti
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.ordinærArbeid
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.permitteringFiskeforedling
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
    private val repository: KjedeOpplysningerRepository,
) {
    fun hentRettighetsperioder(
        ident: String,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate?,
    ): List<RettighetsperiodeResponsDTO> {
        val ønsketPeriode = Datoperiode(fraOgMed, tilOgMed ?: LocalDate.MAX)
        return repository
            .hentRelevanteOpplysninger(ident, relevanteOpplysningstyper)
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
            .hentRelevanteOpplysninger(ident, relevanteOpplysningstyper)
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
                ordinærArbeid.id.uuid to Rettighetstype.ORDINÆR,
                erPermittert.id.uuid to Rettighetstype.PERMITTERING,
                lønnsgaranti.id.uuid to Rettighetstype.LØNNSGARANTI,
                permitteringFiskeforedling.id.uuid to Rettighetstype.FISK,
            )

        /**
         * Opplysningstypene som trengs for rettighetsperioder, rettighetstyper og
         * utbetalinger (det samme som legges i behandlingsresultat-eventet).
         */
        private val relevanteOpplysningstyper: Set<Opplysningstype<*>> =
            setOf(
                // Rettighetsperioder (RegelverkDagpenger.rettighetsperioder)
                KravPåDagpenger.harLøpendeRett,
                // Utbetalinger (RegelverkDagpenger.utbetalinger)
                Beregning.meldeperiode,
                Beregning.utbetaling,
                DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg,
                // Gjenstående dager (+ fallback)
                Beregning.gjenståendeDager,
                Dagpengeperiode.antallStønadsdager,
                // Rettighetstyper for ytelseType-mapping
                ordinærArbeid,
                erPermittert,
                lønnsgaranti,
                permitteringFiskeforedling,
            )
    }
}
