package no.nav.dagpenger.mediator.repository

import kotliquery.Row
import kotliquery.queryOf
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.modell.Ident
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class TidslinjeRepository(
    private val dbSession: DatabaseSession,
) {
    fun hentTidslinje(ident: Ident): PersonTidslinje =
        dbSession.session { session ->
            val identer = ident.alleIdentifikatorer().toTypedArray()

            // language=PostgreSQL
            val behandlingerQuery =
                """
                SELECT
                    b.behandling_id,
                    b.basert_på_behandling_id,
                    bh.forretningsprosess,
                    bh.skjedde AS hendelse_tidspunkt,
                    b.tilstand,
                    b.sist_endret_tilstand,
                    b.opprettet
                FROM behandling b
                JOIN person_behandling pb ON pb.behandling_id = b.behandling_id
                JOIN behandler_hendelse_behandling bhb ON bhb.behandling_id = b.behandling_id
                JOIN behandler_hendelse bh ON bh.melding_id = bhb.melding_id
                WHERE pb.ident = ANY(:identer)
                ORDER BY b.opprettet
                """.trimIndent()

            val behandlinger =
                session.run(
                    queryOf(behandlingerQuery, mapOf("identer" to identer))
                        .map { row -> row.tilBehandlingTidslinje() }
                        .asList,
                )

            // language=PostgreSQL
            val rettigheterQuery =
                """
                SELECT virkningsdato, har_rettighet
                FROM rettighetstatus
                WHERE ident = ANY(:identer)
                ORDER BY virkningsdato
                """.trimIndent()

            val rettighetsperioder =
                session.run(
                    queryOf(rettigheterQuery, mapOf("identer" to identer))
                        .map { row ->
                            Rettighetsperiode(
                                fraOgMed = row.localDate("virkningsdato"),
                                harRett = row.boolean("har_rettighet"),
                            )
                        }.asList,
                )

            PersonTidslinje(behandlinger, rettighetsperioder)
        }

    private fun Row.tilBehandlingTidslinje() =
        BehandlingTidslinje(
            behandlingId = uuid("behandling_id"),
            basertPå = uuidOrNull("basert_på_behandling_id"),
            forretningsprosess = string("forretningsprosess"),
            hendelseTidspunkt = zonedDateTime("hendelse_tidspunkt"),
            tilstand = string("tilstand"),
            sistEndretTilstand = zonedDateTime("sist_endret_tilstand"),
            opprettet = zonedDateTime("opprettet"),
        )
}

data class PersonTidslinje(
    val behandlinger: List<BehandlingTidslinje>,
    val rettighetsperioder: List<Rettighetsperiode>,
)

data class Rettighetsperiode(
    val fraOgMed: LocalDate,
    val harRett: Boolean,
)

data class BehandlingTidslinje(
    val behandlingId: UUID,
    val basertPå: UUID? = null,
    val forretningsprosess: String,
    val hendelseTidspunkt: ZonedDateTime,
    val tilstand: String,
    val sistEndretTilstand: ZonedDateTime,
    val opprettet: ZonedDateTime,
) {
    fun hendelseBeskrivelse(): String =
        when (forretningsprosess) {
            "Søknadsprosess" -> "Søknad om dagpenger"
            "Gjenopptakprosess" -> "Gjenopptak av dagpenger"
            "Omgjøringsprosess" -> "Omgjøring"
            "Manuellprosess" -> "Manuell behandling"
            "Meldekortprosess" -> "Meldekort"
            else -> forretningsprosess
        }

    fun status(): String =
        when (tilstand) {
            "Ferdig" -> "Ferdig"
            "Avbrutt" -> "Avbrutt"
            else -> "Under behandling"
        }
}
