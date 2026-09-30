package no.nav.dagpenger.scenario

import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.grunnlag
import org.junit.jupiter.api.Test

class SporbarhetTest {
    @Test
    fun `saksbehandlerendring lagrer hvem og når på opplysningen`() {
        SimulertDagpengerSystem.Companion
            .nyttScenario { inntektSiste12Mnd = 500000 }
            .test {
                person.søkDagpenger(21.juni(2018))
                behovsløsere.løsTilForslag()

                // endreOpplysning returnerer kildeId, så vi slipper å gjette
                val kildeId =
                    saksbehandler.endreOpplysning(
                        grunnlag,
                        verdi = Beløp(300000),
                        begrunnelse = "Inntekt korrigert etter dokumentasjon fra bruker",
                        gyldighetsperiode = Gyldighetsperiode(21.juni(2018)),
                    )

                // 1. Kilden er lagret med hvem, når og hvorfor
                val kilde =
                    dbTestContext.dbSession.session { session ->
                        session.run(
                            queryOf(
                                //language=PostgreSQL
                                """
                                SELECT k.type AS kildetype,
                                       k.opprettet,
                                       k.registrert,
                                       ks.ident,
                                       ks.begrunnelse
                                FROM kilde k
                                         JOIN kilde_saksbehandler ks ON ks.kilde_id = k.id
                                WHERE k.id = :kildeId
                                """.trimIndent(),
                                mapOf("kildeId" to kildeId),
                            ).map {
                                mapOf(
                                    "kildetype" to it.string("kildetype"),
                                    "ident" to it.string("ident"),
                                    "begrunnelse" to it.stringOrNull("begrunnelse"),
                                    "opprettet" to it.localDateTime("opprettet").toString(),
                                    "registrert" to it.localDateTime("registrert").toString(),
                                )
                            }.asSingle,
                        )
                    }

                withClue("Fant ingen kilde med id=$kildeId") { kilde shouldNotBe null }
                kilde!!["kildetype"] shouldBe "Saksbehandlerkilde"
                kilde["ident"] shouldBe "NAV123123"
                kilde["begrunnelse"] shouldBe "Inntekt korrigert etter dokumentasjon fra bruker"
                kilde["opprettet"] shouldNotBe null
                kilde["registrert"] shouldNotBe null

                // 2. Kilden er faktisk koblet til en opplysning i denne behandlingen
                val antallOpplysninger =
                    dbTestContext.dbSession.session { session ->
                        session.run(
                            queryOf(
                                //language=PostgreSQL
                                """
                                SELECT count(*) AS antall
                                FROM behandling_opplysninger bo
                                         JOIN opplysning o ON o.opplysninger_id = bo.opplysninger_id
                                WHERE bo.behandling_id = :behandlingId
                                  AND o.kilde_id = :kildeId
                                """.trimIndent(),
                                mapOf(
                                    "behandlingId" to person.behandlingId,
                                    "kildeId" to kildeId,
                                ),
                            ).map { it.int("antall") }.asSingle,
                        )
                    }

                withClue("Kilden er ikke koblet til noen opplysning i behandlingen") {
                    antallOpplysninger!! shouldBeGreaterThan 0
                }
            }
    }
}
