package no.nav.dagpenger.mediator.repository

import kotliquery.Session
import kotliquery.queryOf
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.modell.BehandlingskjedeOpplysninger
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
 * Dedikerte lesespørringer for å svare på personnivå-spørsmål (rettighetsperioder,
 * beregninger) uten å gjenoppbygge hele personaggregatet.
 *
 * Til forskjell fra [PersonRepositoryPostgres.hent] (FOR UPDATE-lås + rekursiv
 * opplysningsgraf) henter disse spørringene kun relevante opplysningstyper,
 * flatt per behandlingskjede. Siden alle relevante opplysninger i kjeden
 * slås sammen til ett sett, får vi samme effektive opplysninger som en fullt
 * rehydrert behandling (egne + arvede), og RegelverkDagpenger sine
 * beregningsfunksjoner kan kjøres direkte på settet.
 *
 * Deserialiseringen gjenbruker [somOpplysningRad] og [somOpplysninger] fra
 * OpplysningerRepositoryPostgres.
 */
interface PersonOpplysningerRepository {
    /**
     * Henter relevante opplysninger for alle behandlinger i alle kjedene til en person.
     * Returnerer én Opplysninger-instans per behandlingskjede (flatt, uten arv-mekanikk).
     */
    fun hentRelevanteOpplysninger(ident: String): List<BehandlingskjedeOpplysninger>
}

internal class PersonOpplysningerRepositoryPostgres(
    private val dbSession: DatabaseSession,
    private val kildeRepository: KildeRepository,
    private val opplysningstypeRegister: OpplysningstypeRegister,
) : PersonOpplysningerRepository {
    private companion object {
        /**
         * Opplysningstypene som trengs for rettighetsperioder, rettighetstyper og
         * utbetalinger (det samme som legges i behandlingsresultat-eventet).
         */
        private val relevanteOpplysningstyper: Set<UUID> =
            setOf(
                // Rettighetsperioder (RegelverkDagpenger.rettighetsperioder)
                KravPåDagpenger.harLøpendeRett.id.uuid,
                // Utbetalinger (RegelverkDagpenger.utbetalinger)
                Beregning.meldeperiode.id.uuid,
                Beregning.utbetaling.id.uuid,
                DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg.id.uuid,
                // Gjenstående dager (+ fallback)
                Beregning.gjenståendeDager.id.uuid,
                Dagpengeperiode.antallStønadsdager.id.uuid,
                // Rettighetstyper for ytelseType-mapping
                OpplysningsTyper.HarRettTilOrdinærId.uuid,
                OpplysningsTyper.PermittertId.uuid,
                OpplysningsTyper.LønnsgarantiId.uuid,
                OpplysningsTyper.PermittertFiskeforedlingId.uuid,
            )
    }

    override fun hentRelevanteOpplysninger(ident: String): List<BehandlingskjedeOpplysninger> =
        dbSession.session { session ->
            val opplysningerPerKjede = session.hentRelevanteOpplysninger(ident)
            opplysningerPerKjede.map { (kjedeId, opplysninger) ->
                BehandlingskjedeOpplysninger(
                    behandlingskjedeId = kjedeId,
                    opplysninger = Opplysninger.rehydrer(kjedeId, opplysninger),
                )
            }
        }

    private fun Session.hentRelevanteOpplysninger(ident: String): Map<UUID, List<Opplysning<out Any>>> {
        val rader: List<Pair<UUID, OpplysningRad<out Any>>> =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        WITH RECURSIVE personens_kjeder AS (
                            -- rotbehandlinger (behandlingskjedeId = egen id) for personen
                            SELECT b.behandling_id AS kjede_id, b.behandling_id
                            FROM behandling b
                            INNER JOIN person_behandling pb ON pb.behandling_id = b.behandling_id
                            WHERE pb.ident = :ident AND b.basert_på_behandling_id IS NULL

                            UNION ALL

                            -- alle behandlinger nedover i kjeden
                            SELECT pk.kjede_id, b.behandling_id
                            FROM behandling b
                            INNER JOIN personens_kjeder pk ON b.basert_på_behandling_id = pk.behandling_id
                        )
                        SELECT pk.kjede_id, o.*
                        FROM personens_kjeder pk
                        INNER JOIN behandling b ON b.behandling_id = pk.behandling_id AND b.tilstand = 'Ferdig'
                        INNER JOIN behandling_opplysninger bo ON bo.behandling_id = pk.behandling_id
                        INNER JOIN opplysningstabell o ON o.opplysninger_id = bo.opplysninger_id
                        WHERE o.type_uuid = ANY(:typer)
                        ORDER BY o.id
                        """.trimIndent(),
                        mapOf(
                            "ident" to ident,
                            "typer" to connection.underlying.createArrayOf("uuid", relevanteOpplysningstyper.toTypedArray()),
                        ),
                    ).map { row ->
                        row.uuid("kjede_id") to
                            row.somOpplysningRad(Datatype.fromString(row.string("datatype")), opplysningstypeRegister)
                    }.asList,
                )

        // Hent inn kilde for alle opplysninger vi trenger
        val kilder = kildeRepository.hentKilder(rader.mapNotNull { it.second.kildeId }, this)
        return rader
            .map { (kjedeId, rad) ->
                val kilde = rad.kildeId?.let { kilder[it] ?: throw IllegalStateException("Mangler kilde") }
                kjedeId to rad.copy(kilde = kilde)
            }.groupBy({ it.first }, { it.second })
            .mapValues { (_, raderForKjede) -> raderForKjede.somOpplysninger() }
    }
}
