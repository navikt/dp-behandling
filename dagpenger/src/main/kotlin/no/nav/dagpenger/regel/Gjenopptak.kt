package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi.Companion.basertPå
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilUker
import no.nav.dagpenger.opplysning.regel.dato.nesteStønadsdag
import no.nav.dagpenger.opplysning.regel.dato.sisteAv
import no.nav.dagpenger.opplysning.regel.har
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.regel.størreEnn
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper.førsteAvbruddsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.harForbruktId
import no.nav.dagpenger.regel.OpplysningsTyper.harGjenståendeId
import no.nav.dagpenger.regel.OpplysningsTyper.harSøktInnenFristId
import no.nav.dagpenger.regel.OpplysningsTyper.minimumGjenståendeDagerId
import no.nav.dagpenger.regel.OpplysningsTyper.oppholdMedArbeidI12ukerEllerMerId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteDatoForKravTilGjenopptakId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteforbruksdagId
import no.nav.dagpenger.regel.OpplysningsTyper.skalGjenopptasId
import no.nav.dagpenger.regel.OpplysningsTyper.terskelUkerNyttGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.terskelUkerSidenSistForbrukId
import no.nav.dagpenger.regel.Rettighetstype.skalGjenopptakVurderes
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.sisteGjenståendeDager

object Gjenopptak {
    val oppholdMedArbeidI12ukerEllerMer =
        Opplysningstype.boolsk(
            oppholdMedArbeidI12ukerEllerMerId,
            "Har hatt opphold med arbeid i 12 uker eller mer",
        )
    private val terskelUkerNyttGrunnlag =
        heltall(terskelUkerNyttGrunnlagId, "Antall uker med arbeid for nytt grunnlag", enhet = Enhet.Uker, synlig = aldriSynlig)

    private val antallUker =
        heltall(terskelUkerSidenSistForbrukId, "Kravet til antall uker før gjenopptak", enhet = Enhet.Uker, synlig = aldriSynlig)

    val gjenopptaksdato = Søknadstidspunkt.prøvingsdato

    private val førsteAvbruddsdag = Opplysningstype.dato(førsteAvbruddsdagId, "Første virkedag i avbruddsperiode")
    private val sisteDatoForKravTilGjenopptak = Opplysningstype.dato(sisteDatoForKravTilGjenopptakId, "Siste mulige dato for gjenopptak")

    private val sisteForbruksdag = Opplysningstype.dato(sisteforbruksdagId, "Siste dag med forbruk")

    val minimumGjenståendeDager =
        heltall(minimumGjenståendeDagerId, "Antall gjenstående dager for å kunne gjenoppta", enhet = Enhet.Dager, synlig = aldriSynlig)
    val harForbrukt = Opplysningstype.boolsk(harForbruktId, "Har startet forbruk av stønadsperiode")
    val harGjenstående = Opplysningstype.boolsk(harGjenståendeId, "Har periode igjen å gjenoppta")
    val harSøktInnenFrist =
        Opplysningstype.boolsk(
            harSøktInnenFristId,
            "Har søkt innen fristen for gjenopptak",
            gyldighetsperiode = basertPå(gjenopptaksdato),
        )

    val skalGjenopptas = Opplysningstype.boolsk(skalGjenopptasId, "Oppfyller kravet for gjenopptak av stønadsperiode")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 16, "Gjenopptak av løpende stønadsperiode", "Gjenopptak"),
        ) {
            skalVurderes { opplysninger -> opplysninger.erSann(skalGjenopptakVurderes) }

            // Sjekk at perioden har vært påstartet med forbruk
            regel(harForbrukt) { har(sisteForbruksdag) }

            // Sjekk at det er gjenstående dager å gjenoppta
            regel(minimumGjenståendeDager) { somUtgangspunkt(1) }
            regel(harGjenstående) { størreEnn(sisteGjenståendeDager, minimumGjenståendeDager) }

            // Sjekk om avbruddet har vart lenge nok til å gi nytt grunnlag
            regel(terskelUkerNyttGrunnlag) { somUtgangspunkt(12) }
            regel(oppholdMedArbeidI12ukerEllerMer) { somUtgangspunkt(false) }

            // Sjekk at det har gått nok tid siden siste forbruk til at det kan gjenopptas
            regel(antallUker) { somUtgangspunkt(52) }
            regel(sisteForbruksdag) { sisteAv(Beregning.sisteForbruksdag) }
            regel(førsteAvbruddsdag) { nesteStønadsdag(sisteForbruksdag) }
            regel(sisteDatoForKravTilGjenopptak) { leggTilUker(førsteAvbruddsdag, antallUker) }
            regel(harSøktInnenFrist) { førEllerLik(gjenopptaksdato, sisteDatoForKravTilGjenopptak) }

            utfall(skalGjenopptas) { alle(harForbrukt, harGjenstående, harSøktInnenFrist, skalGjenopptakVurderes) }

            ønsketResultat(terskelUkerNyttGrunnlag, oppholdMedArbeidI12ukerEllerMer)

            påvirkerResultat { it.har(skalGjenopptas) }
        }
}
