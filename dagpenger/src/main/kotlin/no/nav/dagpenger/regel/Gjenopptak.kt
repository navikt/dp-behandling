package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.førsteArbeidsdag
import no.nav.dagpenger.opplysning.regel.dato.leggTilUker
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
import java.time.LocalDate

object Gjenopptak {
    val oppholdMedArbeidI12ukerEllerMer =
        Opplysningstype.boolsk(
            oppholdMedArbeidI12ukerEllerMerId,
            "Har hatt opphold med arbeid i 12 uker eller mer",
        )
    private val terskelUkerNyttGrunnlag =
        Opplysningstype.heltall(terskelUkerNyttGrunnlagId, "Antall uker med arbeid for nytt grunnlag", enhet = Enhet.Uker)

    private val antallUker =
        Opplysningstype.heltall(terskelUkerSidenSistForbrukId, "Kravet til antall uker før gjenopptak", enhet = Enhet.Uker)

    val gjenopptaksdato = Søknadstidspunkt.prøvingsdato

    private val førsteAvbruddsdag = Opplysningstype.dato(førsteAvbruddsdagId, "Første virkedag i avbruddsperiode")
    private val sisteDatoForKravTilGjenopptak = Opplysningstype.dato(sisteDatoForKravTilGjenopptakId, "Siste mulige dato for gjenopptak")

    private val sisteForbruksdag = Opplysningstype.dato(sisteforbruksdagId, "Dagen for siste forbruksdag")

    val minimumGjenståendeDager =
        Opplysningstype.heltall(
            minimumGjenståendeDagerId,
            "Antall gjenstående dager for å kunne gjenoppta",
            enhet = Enhet.Dager,
        )
    val harForbrukt = Opplysningstype.boolsk(harForbruktId, "Har startet forbruk av stønadsperiode")
    val harGjenstående = Opplysningstype.boolsk(harGjenståendeId, "Har periode igjen å gjenoppta")
    val harSøktInnenFrist =
        Opplysningstype.boolsk(
            harSøktInnenFristId,
            "Har søkt innen fristen for gjenopptak",
            gyldighetsperiode =
                GyldighetsperiodeStrategi { _, basertPå ->
                    val fomDato =
                        basertPå.find { it.opplysningstype == gjenopptaksdato }?.verdi
                            ?: throw IllegalStateException("Fant ikke gjenopptaksdato i basertPå")
                    require(fomDato is LocalDate) { "Gjenopptaksdato må være av typen LocalDate" }
                    Gyldighetsperiode(
                        fom = fomDato,
                    )
                },
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
            regel(førsteAvbruddsdag) { førsteArbeidsdag(sisteForbruksdag) }
            regel(sisteDatoForKravTilGjenopptak) { leggTilUker(førsteAvbruddsdag, antallUker) }
            regel(harSøktInnenFrist) { førEllerLik(gjenopptaksdato, sisteDatoForKravTilGjenopptak) }

            utfall(skalGjenopptas) { alle(harForbrukt, harGjenstående, harSøktInnenFrist, skalGjenopptakVurderes) }

            ønsketResultat(terskelUkerNyttGrunnlag, oppholdMedArbeidI12ukerEllerMer)

            påvirkerResultat { it.har(skalGjenopptas) }
        }
}
