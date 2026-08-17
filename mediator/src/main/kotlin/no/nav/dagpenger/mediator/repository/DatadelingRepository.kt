package no.nav.dagpenger.mediator.repository

import kotliquery.Session
import kotliquery.queryOf
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.OpplysningstypeRegister
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import java.util.UUID

/**
 * Dedikerte lesespørringer for datadeling.
 *
 * Til forskjell fra [PersonRepositoryPostgres.hent] som gjenoppbygger hele
 * personaggregatet (med FOR UPDATE-lås og rekursive opplysningsgrafer), henter
 * disse spørringene kun de opplysningstypene som trengs for å bygge
 * perioder og beregninger — direkte fra opplysningstabellen.
 *
 * Deserialiseringen av opplysninger gjenbruker [somOpplysningRad] og
 * [somOpplysninger] fra OpplysningerRepositoryPostgres.
 */
interface DatadelingRepository {
    fun hentFerdigeBehandlinger(ident: String): List<BehandlingMedOpplysninger>
}

data class BehandlingMedOpplysninger(
    val behandlingId: UUID,
    val opplysninger: Opplysninger,
)

internal class DatadelingRepositoryPostgres(
    private val dbSession: DatabaseSession,
    private val kildeRepository: KildeRepository,
    private val opplysningstypeRegister: OpplysningstypeRegister,
) : DatadelingRepository {
    private companion object {
        /**
         * Opplysningstypene som trengs for å reprodusere innholdet i
         * behandlingsresultat-eventet slik dp-datadeling tolker det:
         * rettighetsperioder, rettighetstyper og utbetalinger.
         */
        private val relevanteOpplysningstyper: Set<UUID> =
            setOf(
                // Rettighetsperioder (RegelverkDagpenger.rettighetsperioder)
                KravPåDagpenger.harLøpendeRett.id.uuid,
                // Utbetalinger (RegelverkDagpenger.utbetalinger)
                Beregning.meldeperiode.id.uuid,
                Beregning.utbetaling.id.uuid,
                DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg.id.uuid,
                // Gjenstående dager (+ fallback slik dp-datadeling tolker det)
                Beregning.gjenståendeDager.id.uuid,
                Dagpengeperiode.antallStønadsdager.id.uuid,
                // Rettighetstyper for ytelseType-mapping (samme UUID-er som dp-datadeling hardkoder)
                OpplysningsTyper.HarRettTilOrdinærId.uuid,
                OpplysningsTyper.PermittertId.uuid,
                OpplysningsTyper.LønnsgarantiId.uuid,
                OpplysningsTyper.PermittertFiskeforedlingId.uuid,
            )
    }

    override fun hentFerdigeBehandlinger(ident: String): List<BehandlingMedOpplysninger> =
        dbSession.session { session ->
            val behandlinger = session.hentFerdigeBehandlinger(ident)
            val opplysningerPerBehandling = session.hentRelevanteOpplysninger(behandlinger.values.toSet())

            behandlinger.map { (behandlingId, opplysningerId) ->
                BehandlingMedOpplysninger(
                    behandlingId = behandlingId,
                    opplysninger =
                        Opplysninger.rehydrer(
                            opplysningerId,
                            opplysningerPerBehandling[opplysningerId] ?: emptyList(),
                        ),
                )
            }
        }

    private fun Session.hentFerdigeBehandlinger(ident: String): Map<UUID, UUID> =
        this
            .run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT b.behandling_id, bo.opplysninger_id
                    FROM person_behandling pb
                    INNER JOIN behandling b ON b.behandling_id = pb.behandling_id
                    INNER JOIN behandling_opplysninger bo ON bo.behandling_id = b.behandling_id
                    WHERE pb.ident = :ident
                      AND b.tilstand = 'Ferdig'
                    """.trimIndent(),
                    mapOf("ident" to ident),
                ).map { row -> row.uuid("behandling_id") to row.uuid("opplysninger_id") }.asList,
            ).toMap()

    private fun Session.hentRelevanteOpplysninger(opplysningerIder: Set<UUID>): Map<UUID, List<Opplysning<out Any>>> {
        if (opplysningerIder.isEmpty()) return emptyMap()

        val rader: List<OpplysningRad<out Any>> =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT o.*
                        FROM opplysningstabell o
                        WHERE o.opplysninger_id = ANY(:opplysninger_ider)
                          AND o.type_uuid = ANY(:typer)
                        ORDER BY o.id
                        """.trimIndent(),
                        mapOf(
                            "opplysninger_ider" to connection.underlying.createArrayOf("uuid", opplysningerIder.toTypedArray()),
                            "typer" to connection.underlying.createArrayOf("uuid", relevanteOpplysningstyper.toTypedArray()),
                        ),
                    ).map { row ->
                        row.somOpplysningRad(Datatype.fromString(row.string("datatype")), opplysningstypeRegister)
                    }.asList,
                )

        // Hent inn kilde for alle opplysninger vi trenger (samme mønster som OpplysningerRepositoryPostgres)
        val kilder = kildeRepository.hentKilder(rader.mapNotNull { it.kildeId }, this)
        val raderMedKilde =
            rader.map {
                if (it.kildeId == null) return@map it
                val kilde = kilder[it.kildeId] ?: throw IllegalStateException("Mangler kilde")
                it.copy(kilde = kilde)
            }

        return raderMedKilde
            .groupBy { it.opplysingerId }
            .mapValues { (_, raderForBehandling) -> raderForBehandling.somOpplysninger() }
    }
}
