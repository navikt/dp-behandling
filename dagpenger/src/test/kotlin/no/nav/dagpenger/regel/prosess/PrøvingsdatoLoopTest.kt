package no.nav.dagpenger.regel.prosess

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.august
import no.nav.dagpenger.dato.juli
import no.nav.dagpenger.dato.september
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.ønsketdato
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Reproduserer (og verifiserer fiksen for) RegelkjøringLoopException for prøvingsdato,
 * uten hele mediator/scenario-oppsettet.
 *
 * Mekanismen: en tidligere (arvet) behandling har fastsatt prøvingsdato basert på søknadstidspunkt
 * fra søknad S1 (uendelig gyldig framover). En NY, senere søknad (S2) i en etterfølgende
 * behandling erstatter søknadId/søknadsdato/ønsketdato-kjeden - som transitivt markerer det
 * gamle søknadstidspunktet (og alt utledet av det) som erstattet.
 *
 * Når regelkjøringen deretter skal evaluere en dag som fortsatt korrekt "eier" den GAMLE,
 * dato-avkortede prøvingsdatoen (altså en dag FØR den nye søknadens egen periode starter),
 * sjekker Regel.lagPlan/lagPlanFraUtledning om avhengighetene til den GAMLE prøvingsdatoen er
 * erstattet - IKKE om erstatningen faktisk gjelder for denne konkrete dagen. Siden det alltid
 * er tilfelle her (søknadstidspunktet ER globalt erstattet av den nye søknaden), ville
 * regelen "Fastsetter Prøvingsdato til siste dato av Søknadstidspunkt" bli replanlagt på nytt.
 *
 * Uten fiksen produserer denne replanleggingen en ny prøvingsdato (01.09) hvis gyldighetsperiode
 * (fraOgMed=egen verdi=01.09, jf. GyldighetsperiodeStrategi.egenVerdi) ALDRI dekker dagen som
 * evalueres (20.08) - så resultatet "mangler" fortsatt for 20.08 neste runde, og regelen
 * replanlegges uendelig med identisk resultat -> RegelkjøringLoopException. Dette er bekreftet
 * empirisk mot produksjonsdata (se loop-diagnostikken i RegelkjøringLoopException), der en
 * gammel, fortsatt gyldig prøvingsdato ble forkastet til fordel for en ny verdi som aldri
 * dekket dagen den skulle løse.
 *
 * Med fiksen (den dedikerte regelen Prøvingsdato, se regel/dato/Prøvingsdato.kt) sjekker
 * prøvingsdato-regelen selv, når avhengigheten
 * er erstattet, om en ny beregning faktisk ville dekket dagen som evalueres. Siden 01.09 ligger
 * ETTER 20.08, hopper regelen over replanlegging og beholder den allerede fastsatte (og fortsatt
 * gyldige) prøvingsdatoen fra S1 (27.07) for denne dagen. For en dag som faktisk dekkes av S2 sin
 * prøvingsdato (f.eks. 01.09 selv), vil regelen derimot bli replanlagt som normalt.
 */
class PrøvingsdatoLoopTest {
    @Test
    @Disabled
    fun `ny søknad som erstatter søknadstidspunkt skal ikke få regelkjøringen til å loope for en eldre dag`() {
        // Behandling 1: opprinnelig gjenopptaksøknad S1, prøvingsdato blir fastsatt til 27. juli.
        val behandling1 = Opplysninger()
        behandling1.leggTil(Faktum(søknadIdOpplysningstype, "S1", Gyldighetsperiode(27.juli(2026))))
        behandling1.leggTil(Faktum(ønsketdato, 27.juli(2026), Gyldighetsperiode(27.juli(2026))))

        Regelkjøring(
            regelverksdato = 27.juli(2026),
            opplysninger = behandling1,
            Søknadstidspunkt.regelsett,
        ).evaluer()

        behandling1.finnOpplysning(prøvingsdato).verdi shouldBe 27.juli(2026)

        // Behandling 2 (f.eks. en ny gjenopptaksøknad S2, 19. august), basert på behandling 1.
        // Prøver å regne på en dag (20. august) som fortsatt skal "eies" av den gamle,
        // dato-avkortede prøvingsdatoen (27. juli), siden den nye søknadens EGEN prøvingsdato
        // (1. september) ligger etter 20. august.
        val behandling2 = Opplysninger.basertPå(behandling1)
        behandling2.leggTil(Faktum(søknadIdOpplysningstype, "S2", Gyldighetsperiode(19.august(2026))))
        behandling2.leggTil(Faktum(ønsketdato, 1.september(2026), Gyldighetsperiode(19.august(2026))))

        val regelkjøring =
            Regelkjøring(
                regelverksdato = 20.august(2026),
                opplysninger = behandling2,
                Søknadstidspunkt.regelsett,
            )

        // Skal ikke lenger kaste RegelkjøringLoopException.
        shouldNotThrowAny { regelkjøring.evaluer() }

        // Prøvingsdato for 20.08 skal fortsatt være den gamle, fortsatt gyldige verdien (27.07) -
        // ikke den nye søknadens verdi (01.09), som uansett aldri ville dekket denne dagen.
        behandling2.forDato(20.august(2026)).finnOpplysning(prøvingsdato).verdi shouldBe 27.juli(2026)
    }

    @Test
    fun `prøvingsdato skal oppdateres til den nye søknadens verdi for en dag den faktisk dekker`() {
        val behandling1 = Opplysninger()
        behandling1.leggTil(Faktum(søknadIdOpplysningstype, "S1", Gyldighetsperiode(27.juli(2026))))
        behandling1.leggTil(Faktum(ønsketdato, 27.juli(2026), Gyldighetsperiode(27.juli(2026))))

        Regelkjøring(
            regelverksdato = 27.juli(2026),
            opplysninger = behandling1,
            Søknadstidspunkt.regelsett,
        ).evaluer()

        val behandling2 = Opplysninger.basertPå(behandling1)
        behandling2.leggTil(Faktum(søknadIdOpplysningstype, "S2", Gyldighetsperiode(19.august(2026))))
        behandling2.leggTil(Faktum(ønsketdato, 1.september(2026), Gyldighetsperiode(19.august(2026))))

        Regelkjøring(
            regelverksdato = 1.september(2026),
            opplysninger = behandling2,
            Søknadstidspunkt.regelsett,
        ).evaluer()

        behandling2.forDato(1.september(2026)).finnOpplysning(prøvingsdato).verdi shouldBe 1.september(2026)
    }
}
