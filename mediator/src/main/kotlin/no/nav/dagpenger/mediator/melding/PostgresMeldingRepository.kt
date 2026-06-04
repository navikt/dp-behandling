package no.nav.dagpenger.mediator.melding

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.dagpenger.ferietillegg.mottak.BeregnFerietilleggMottak.BeregnFerietilleggMessage
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.mediator.mottak.AvbrytBehandlingMessage
import no.nav.dagpenger.mediator.mottak.AvklaringIkkeRelevantMessage
import no.nav.dagpenger.mediator.mottak.BehandlingStårFastMessage
import no.nav.dagpenger.mediator.mottak.BeregnMeldekortMottak.BeregnMeldekortMessage
import no.nav.dagpenger.mediator.mottak.FjernOpplysningMessage
import no.nav.dagpenger.mediator.mottak.FlyttBehandlingMottak.FlyttBehandlingMessage
import no.nav.dagpenger.mediator.mottak.GodkjennBehandlingMessage
import no.nav.dagpenger.mediator.mottak.MeldekortInnsendtMessage
import no.nav.dagpenger.mediator.mottak.OmgjøringMessage
import no.nav.dagpenger.mediator.mottak.OpplysningSvarMessage
import no.nav.dagpenger.mediator.mottak.UtbetalingStatusMessage
import no.nav.dagpenger.mediator.repository.ApiMelding
import no.nav.dagpenger.regel.mottak.AvsluttetArbeidssøkerperiodeMottak.AvsluttetArbeidssøkerperiodeMessage
import no.nav.dagpenger.regel.mottak.OpprettBehandlingMessage
import no.nav.dagpenger.regel.mottak.SamordningHendelseMottak.SamordningHendelseMessage
import no.nav.dagpenger.regel.mottak.SøknadInnsendtMessage
import no.nav.dagpenger.regelverk.melding.Melding
import no.nav.dagpenger.regelverk.melding.MeldingRepository
import org.postgresql.util.PGobject
import java.util.UUID

internal class PostgresMeldingRepository(
    val dbSession: DatabaseSession,
) : MeldingRepository {
    override fun lagreMelding(
        melding: Melding,
        ident: String,
        id: UUID,
        toJson: String,
    ) {
        val hendelseType = meldingType(melding) ?: return

        dbSession.session { session ->
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
        dbSession.session { session ->
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
        dbSession.session { session ->
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
            is ApiMelding -> {
                MeldingTypeDTO.API
            }

            is AvbrytBehandlingMessage -> {
                MeldingTypeDTO.AVBRYT_BEHANDLING
            }

            is SamordningHendelseMessage -> {
                MeldingTypeDTO.SAMORDNING_HENDELSE
            }

            is AvklaringIkkeRelevantMessage -> {
                MeldingTypeDTO.AVKLARING_IKKE_RELEVANT
            }

            is BehandlingStårFastMessage -> {
                MeldingTypeDTO.BEHANDLING_STÅR_FAST
            }

            is AvsluttetArbeidssøkerperiodeMessage -> {
                MeldingTypeDTO.AVSLUTTET_ARBEIDSSØKERPERIODE
            }

            is BeregnFerietilleggMessage -> {
                MeldingTypeDTO.FERIETILLEGG
            }

            is BeregnMeldekortMessage -> {
                MeldingTypeDTO.BEREGN_MELDEKORT
            }

            is FjernOpplysningMessage -> {
                MeldingTypeDTO.FJERN_OPPLYSNING
            }

            is GodkjennBehandlingMessage -> {
                MeldingTypeDTO.MANUELL_BEHANDLING_AVKLART
            }

            is MeldekortInnsendtMessage -> {
                MeldingTypeDTO.MELDEKORT_INNSENDT
            }

            is OmgjøringMessage -> {
                MeldingTypeDTO.OMGJØRING
            }

            is OpplysningSvarMessage -> {
                MeldingTypeDTO.OPPLYSNING_SVAR
            }

            is OpprettBehandlingMessage -> {
                MeldingTypeDTO.OPPRETT_BEHANDLING
            }

            is SøknadInnsendtMessage -> {
                MeldingTypeDTO.SØKNAD_INNSENDT
            }

            is UtbetalingStatusMessage -> {
                MeldingTypeDTO.UTBETALING_STATUS
            }

            is FlyttBehandlingMessage -> {
                MeldingTypeDTO.FLYTT_BEHANDLING
            }

            else -> {
                null.also {
                    logger.warn { "ukjent meldingstype ${hendelseMessage::class.simpleName}: melding lagres ikke" }
                }
            }
        }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}

private enum class MeldingTypeDTO {
    API,
    AVBRYT_BEHANDLING,
    AVKLARING_IKKE_RELEVANT,
    AVSLUTTET_ARBEIDSSØKERPERIODE,
    BEHANDLING_STÅR_FAST,
    BEREGN_MELDEKORT,
    FERIETILLEGG,
    FJERN_OPPLYSNING,
    FLYTT_BEHANDLING,
    MANUELL_BEHANDLING_AVKLART,
    MELDEKORT_INNSENDT,
    OMGJØRING,
    OPPLYSNING_SVAR,
    OPPRETT_BEHANDLING,
    SAMORDNING_HENDELSE,
    SØKNAD_INNSENDT,
    UTBETALING_STATUS,
}
