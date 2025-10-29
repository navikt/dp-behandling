package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilUker
import no.nav.dagpenger.opplysning.regel.dato.sisteAv
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.OpplysningsTyper.antallDagerForbruktId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteDatoForKravTilGjenopptakId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteforbruksdagId
import no.nav.dagpenger.regel.OpplysningsTyper.skalGjenopptasId
import no.nav.dagpenger.regel.beregning.Beregning

object Gjenopptak {
    val skalGjenopptas = Opplysningstype.boolsk(skalGjenopptasId, "Skal gjenopptas?")

    private val antallUker =
        Opplysningstype.heltall(antallDagerForbruktId, "Kravet til antall uker før gjenopptak")

    private val gjennoptaksdato = Søknadstidspunkt.prøvingsdato
    private val sisteDatoForKravTilGjenopptak =
        Opplysningstype.dato(
            sisteDatoForKravTilGjenopptakId,
            "Siste mulige dato for gjenopptak",
        )

    private val forbruktedager = Beregning.forbrukt
    private val sisteForbruksdag = Opplysningstype.dato(sisteforbruksdagId, "Dagen for siste forbruksdag")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 16, "Gjenopptak av løpende stønadsperiode", "Gjenopptak"),
        ) {
            skalVurderes { opplysninger ->
                opplysninger.har(forbruktedager)
            }

            regel(antallUker) { somUtgangspunkt(52) }
            regel(sisteForbruksdag) { sisteAv(forbruktedager) }
            regel(sisteDatoForKravTilGjenopptak) { leggTilUker(sisteForbruksdag, antallUker) }
            utfall(skalGjenopptas) { førEllerLik(gjennoptaksdato, sisteDatoForKravTilGjenopptak) }
        }
}
