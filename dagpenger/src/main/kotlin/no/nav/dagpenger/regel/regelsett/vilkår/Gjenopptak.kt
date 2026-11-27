package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi.Companion.basertPå
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.dato.etterEllerLik
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilUker
import no.nav.dagpenger.opplysning.regel.dato.nesteStønadsdag
import no.nav.dagpenger.opplysning.regel.dato.sisteAv
import no.nav.dagpenger.opplysning.regel.erHøyereEnnForrige
import no.nav.dagpenger.opplysning.regel.har
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.regel.størreEnn
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.Avklaringspunkter.GjenopptakEtter12Uker
import no.nav.dagpenger.regel.OpplysningsTyper.avbruddSkyldesArbeidId
import no.nav.dagpenger.regel.OpplysningsTyper.dagpengegrunnlagErHøyereEnnForrigeId
import no.nav.dagpenger.regel.OpplysningsTyper.førsteAvbruddsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.grenseForNyttGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.harForbruktId
import no.nav.dagpenger.regel.OpplysningsTyper.harGjenståendeId
import no.nav.dagpenger.regel.OpplysningsTyper.harNyttGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.harSøktInnenFristId
import no.nav.dagpenger.regel.OpplysningsTyper.minimumGjenståendeDagerId
import no.nav.dagpenger.regel.OpplysningsTyper.oppholdMedArbeidI12ukerEllerMerId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteDatoForKravTilGjenopptakId
import no.nav.dagpenger.regel.OpplysningsTyper.sisteforbruksdagId
import no.nav.dagpenger.regel.OpplysningsTyper.skalGjenopptasId
import no.nav.dagpenger.regel.OpplysningsTyper.skalReberegneGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.terskelUkerNyttGrunnlagId
import no.nav.dagpenger.regel.OpplysningsTyper.terskelUkerSidenSistForbrukId
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.sisteGjenståendeDager
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalGjenopptakVurderes

object Gjenopptak {
    private val terskelUkerNyttGrunnlag =
        heltall(terskelUkerNyttGrunnlagId, "Antall uker med arbeid for nytt grunnlag", synlig = aldriSynlig, enhet = Enhet.Uker)
    private val antallUker =
        heltall(terskelUkerSidenSistForbrukId, "Kravet til antall uker før gjenopptak", synlig = aldriSynlig, enhet = Enhet.Uker)
    private val minimumGjenståendeDager =
        heltall(minimumGjenståendeDagerId, "Antall gjenstående dager for å kunne gjenoppta", synlig = aldriSynlig, enhet = Enhet.Dager)

    val gjenopptaksdato = Søknadstidspunkt.prøvingsdato

    // Grensedatoer for gjenopptak
    private val sisteForbruksdag = Opplysningstype.dato(sisteforbruksdagId, "Siste dag med forbruk")
    private val førsteAvbruddsdag = Opplysningstype.dato(førsteAvbruddsdagId, "Første virkedag i avbruddsperiode")
    private val grenseForNyttGrunnlag = Opplysningstype.dato(grenseForNyttGrunnlagId, "Tidligste dato for opphold på 12 uker eller mer")
    private val sisteDatoForKravTilGjenopptak = Opplysningstype.dato(sisteDatoForKravTilGjenopptakId, "Siste mulige dato for gjenopptak")

    // Er det noe å gjenoppta?
    val harForbrukt = Opplysningstype.boolsk(harForbruktId, "Har startet forbruk av stønadsperiode")
    val harGjenstående = Opplysningstype.boolsk(harGjenståendeId, "Har periode igjen å gjenoppta")

    // Er det søkt innen fristen
    val harSøktInnenFrist =
        Opplysningstype.boolsk(
            harSøktInnenFristId,
            "Har søkt innen fristen for gjenopptak",
            gyldighetsperiode = basertPå(gjenopptaksdato),
        )

    // Skal grunnlaget fastsettes på nytt
    val oppholdMedArbeidI12ukerEllerMer =
        Opplysningstype.boolsk(
            oppholdMedArbeidI12ukerEllerMerId,
            "Avbruddet har vart i 12 uker eller mer",
        )

    // Saksbehandler må ta stilling til at avbruddet skyldes arbeid og ikke andre årsaker
    val avbruddSkyldesArbeid = Opplysningstype.boolsk(avbruddSkyldesArbeidId, "Avbruddet skyldes arbeid")

    val skalReberegneGrunnlag =
        Opplysningstype.boolsk(skalReberegneGrunnlagId, "Skal grunnlaget beregnes på nytt etter avbrudd")

    private val dagpengegrunnlagErHøyereEnnForrige =
        Opplysningstype.boolsk(dagpengegrunnlagErHøyereEnnForrigeId, "Nytt dagpengegrunnlag er høyere enn forrige grunnlag")

    // Ble det faktisk fastsatt nytt grunnlag som følge av opphold?
    val harNyttGrunnlag = Opplysningstype.boolsk(harNyttGrunnlagId, "Har fått nytt grunnlag etter opphold")

    val skalGjenopptas = Opplysningstype.boolsk(skalGjenopptasId, "Oppfyller kravet for gjenopptak av stønadsperiode")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 16, "Gjenopptak av løpende stønadsperiode", "Gjenopptak"),
        ) {
            aktivNår(skalGjenopptakVurderes)

            // Sjekk at perioden har vært påstartet med forbruk
            regel(harForbrukt) { har(sisteForbruksdag) }

            // Sjekk at det er gjenstående dager å gjenoppta
            regel(minimumGjenståendeDager) { somUtgangspunkt(1) }
            regel(harGjenstående) { størreEnn(sisteGjenståendeDager, minimumGjenståendeDager) }

            // Sjekk om avbruddet har vart lenge nok til å gi nytt grunnlag
            regel(terskelUkerNyttGrunnlag) { somUtgangspunkt(12) }
            regel(grenseForNyttGrunnlag) { leggTilUker(førsteAvbruddsdag, terskelUkerNyttGrunnlag) }
            regel(oppholdMedArbeidI12ukerEllerMer) { etterEllerLik(gjenopptaksdato, grenseForNyttGrunnlag) }

            // Nytt grunnlag krever både langt nok avbrudd og at avbruddet skyldes arbeid
            regel(avbruddSkyldesArbeid) { somUtgangspunkt(false) }
            regel(skalReberegneGrunnlag) { alle(oppholdMedArbeidI12ukerEllerMer, avbruddSkyldesArbeid) }

            // Nytt grunnlag gjelder bare hvis det ble innhentet nye inntekter og det nye grunnlaget er høyere enn det vi hadde
            regel(dagpengegrunnlagErHøyereEnnForrige) {
                erHøyereEnnForrige(Dagpengegrunnlag.dagpengegrunnlag, Dagpengegrunnlag.grunnlag)
            }
            regel(harNyttGrunnlag) { alle(skalReberegneGrunnlag, dagpengegrunnlagErHøyereEnnForrige) }

            // Sjekk at det har gått nok tid siden siste forbruk til at det kan gjenopptas
            regel(antallUker) { somUtgangspunkt(52) }
            regel(sisteForbruksdag) { sisteAv(Beregning.sisteForbruksdag) }
            regel(førsteAvbruddsdag) { nesteStønadsdag(sisteForbruksdag) }
            regel(sisteDatoForKravTilGjenopptak) { leggTilUker(førsteAvbruddsdag, antallUker) }
            regel(harSøktInnenFrist) { førEllerLik(gjenopptaksdato, sisteDatoForKravTilGjenopptak) }

            utfall(skalGjenopptas) { alle(harForbrukt, harGjenstående, harSøktInnenFrist, skalGjenopptakVurderes) }

            ønsketResultat(
                terskelUkerNyttGrunnlag,
                oppholdMedArbeidI12ukerEllerMer,
                avbruddSkyldesArbeid,
                skalReberegneGrunnlag,
                harNyttGrunnlag,
            )

            avklaring(GjenopptakEtter12Uker)

            påvirkerResultat { it.har(skalGjenopptas) }
        }

    val GjenopptakEtter12UkerKontroll =
        Kontrollpunkt(GjenopptakEtter12Uker) { opplysninger ->
            opplysninger.erSann(oppholdMedArbeidI12ukerEllerMer) &&
                opplysninger.finnNullableOpplysning(avbruddSkyldesArbeid)?.kilde !is Saksbehandlerkilde
        }
}
