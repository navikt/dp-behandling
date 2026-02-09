package no.nav.dagpenger.behandling.mediator.melding

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.mottak.AvbrytBehandlingMessage
import no.nav.dagpenger.behandling.mediator.mottak.AvklaringIkkeRelevantMessage
import no.nav.dagpenger.behandling.mediator.mottak.BehandlingStårFastMessage
import no.nav.dagpenger.behandling.mediator.mottak.BeregnMeldekortMottak.BeregnMeldekortMessage
import no.nav.dagpenger.behandling.mediator.mottak.FjernOpplysningMessage
import no.nav.dagpenger.behandling.mediator.mottak.GodkjennBehandlingMessage
import no.nav.dagpenger.behandling.mediator.mottak.MeldekortInnsendtMessage
import no.nav.dagpenger.behandling.mediator.mottak.OmgjøringMessage
import no.nav.dagpenger.behandling.mediator.mottak.OpplysningSvarMessage
import no.nav.dagpenger.behandling.mediator.mottak.OpprettBehandlingMessage
import no.nav.dagpenger.behandling.mediator.mottak.SøknadInnsendtMessage
import no.nav.dagpenger.behandling.mediator.mottak.UtbetalingStatusMessage
import no.nav.dagpenger.behandling.mediator.repository.ApiMelding
import org.postgresql.util.PGobject
import java.util.UUID

internal class PostgresMeldingRepository : MeldingRepository {
    override fun lagreMelding(
        melding: Melding,
        ident: String,
        id: UUID,
        toJson: String,
    ) {
        val hendelseType = meldingType(melding) ?: return

        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession: TransactionalSession ->
                transactionalSession.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            INSERT INTO melding
                                (ident, melding_id, melding_type, data, lest_dato)
                            VALUES
                                (:ident, :melding_id, :melding_type, :data, NOW())
                            ON CONFLICT DO NOTHING
                            """.trimIndent(),
                        paramMap =
                            mapOf(
                                "ident" to ident,
                                "melding_id" to id,
                                "melding_type" to hendelseType.name,
                                "data" to
                                    PGobject().apply {
                                        type = "json"
                                        value = toJson
                                    },
                            ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun markerSomBehandlet(meldingId: UUID) =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        UPDATE melding
                        SET behandlet_tidspunkt=NOW()
                        WHERE melding_id = :melding_id
                          AND behandlet_tidspunkt IS NULL
                        """.trimIndent(),
                    paramMap = mapOf("melding_id" to meldingId),
                ).asUpdate,
            )
        }

    override fun erBehandlet(meldingId: UUID): Boolean =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT behandlet_tidspunkt FROM melding WHERE melding_id = :melding_id
                        """.trimIndent(),
                    paramMap = mapOf("melding_id" to meldingId),
                ).map { rad -> rad.localDateTimeOrNull("behandlet_tidspunkt") }.asSingle,
            ) != null
        }

    private fun meldingType(hendelseMessage: Melding): MeldingTypeDTO? =
        when (hendelseMessage) {
            is ApiMelding -> MeldingTypeDTO.API
            is AvbrytBehandlingMessage -> MeldingTypeDTO.AVBRYT_BEHANDLING
            is AvklaringIkkeRelevantMessage -> MeldingTypeDTO.AVKLARING_IKKE_RELEVANT
            is BeregnMeldekortMessage -> MeldingTypeDTO.BEREGN_MELDEKORT
            is FjernOpplysningMessage -> MeldingTypeDTO.FJERN_OPPLYSNING
            is GodkjennBehandlingMessage -> MeldingTypeDTO.MANUELL_BEHANDLING_AVKLART
            is MeldekortInnsendtMessage -> MeldingTypeDTO.MELDEKORT_INNSENDT
            is OmgjøringMessage -> MeldingTypeDTO.OMGJØRING
            is OpplysningSvarMessage -> MeldingTypeDTO.OPPLYSNING_SVAR
            is OpprettBehandlingMessage -> MeldingTypeDTO.OPPRETT_BEHANDLING
            is SøknadInnsendtMessage -> MeldingTypeDTO.SØKNAD_INNSENDT
            is UtbetalingStatusMessage -> MeldingTypeDTO.UTBETALING_STATUS
            is BehandlingStårFastMessage -> MeldingTypeDTO.BEHANDLING_STÅR_FAST
            else ->
                null.also {
                    logger.warn { "ukjent meldingstype ${hendelseMessage::class.simpleName}: melding lagres ikke" }
                }
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

private enum class MeldingTypeDTO {
    AVBRYT_BEHANDLING,
    AVKLARING_IKKE_RELEVANT,
    BEREGN_MELDEKORT,
    MANUELL_BEHANDLING_AVKLART,
    OPPLYSNING_SVAR,
    SØKNAD_INNSENDT,
    MELDEKORT_INNSENDT,
    API,
    OPPRETT_BEHANDLING,
    FJERN_OPPLYSNING,
    UTBETALING_STATUS,
    BEHANDLING_STÅR_FAST,
    OMGJØRING,
}
