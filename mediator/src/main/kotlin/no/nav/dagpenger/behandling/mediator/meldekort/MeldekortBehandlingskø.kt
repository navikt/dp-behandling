package no.nav.dagpenger.behandling.mediator.meldekort

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotliquery.sessionOf
import mu.withLoggingContext
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.db.medLås
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator

class MeldekortBehandlingskø(
    private val personRepositoryPostgres: PersonRepository,
    private val meldekortRepository: MeldekortRepository,
    private val rapidsConnection: RapidsConnection,
) {
    companion object {
        private const val LÅSE_NØKKEL = 98769876
        private val logger = mu.KotlinLogging.logger {}
    }

    fun sendMeldekortTilBehandling() {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.medLås(LÅSE_NØKKEL) {
                    val kø = meldekortRepository.hentMeldekortkø()
                    val totalt = kø.behandlingsklare + kø.underBehandling
                    logger.info {
                        "Har funnet ${totalt.size} meldekort," +
                            " ${kø.underBehandling.size} påbegynt og ${kø.behandlingsklare.size} behandlingsklare."
                    }

                    kø.behandlingsklare.map { it.meldekort }.forEach { meldekort ->
                        val meldekortPeriode = meldekort.periode()
                        val rettighetstatus = personRepositoryPostgres.rettighetstatusFor(meldekort.ident.tilPersonIdentfikator())

                        withLoggingContext(
                            "meldekortId" to meldekort.id.toString(),
                        ) {
                            val potensielleDager =
                                meldekortPeriode.associateWith { dag ->
                                    runCatching { rettighetstatus.get(dag).utfall }.getOrElse { false }
                                }

                            val harRettighet = potensielleDager.any { it.value }
                            if (harRettighet) {
                                logger.info { "Publiserer beregn meldekort" }
                                rapidsConnection.publish(
                                    meldekort.ident,
                                    JsonMessage
                                        .newMessage(
                                            "beregn_meldekort",
                                            mapOf(
                                                "meldekortId" to meldekort.id,
                                                "ident" to meldekort.ident,
                                            ),
                                        ).toJson(),
                                )

                                meldekortRepository.behandlingStartet(
                                    meldekort.eksternMeldekortId,
                                )
                            } else {
                                logger.info {
                                    """
                                    Meldekort skal ikke behandles. Ingen overlapp mellom meldekortes fraOgMed=${meldekortPeriode.fraOgMed}, 
                                    og tilOgMed=${meldekortPeriode.tilOgMed} og rettighetstatus. 
                                    """.trimIndent()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
