package no.nav.dagpenger.regel.regelverk

import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.Avgjørelsesberegning
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import java.time.temporal.ChronoUnit

object Avgjørelseberegning : Avgjørelsesberegning {
    override fun avgjørelse(opplysninger: LesbarOpplysninger): Avgjørelse {
        // Perioder kan komme i vilkårlig rekkefølge (f.eks. en etterregistrert/tilbakedatert periode som
        // dukker opp etter en periode som allerede er kjent) - sorter derfor kronologisk før vi resonnerer
        // om hva som faktisk er den gjeldende (siste) statusen.
        val perioder = Rettighetsperioder.rettighetsperioder(opplysninger).sortedBy { it.fraOgMed }
        if (perioder.isEmpty()) return Avgjørelse.Uavklart

        val (nye, arvede) = perioder.partition { it.endret }

        // Ingen nye perioder betyr at forrige avgjørelse videreføres uendret, f.eks. ved meldekort
        if (nye.isEmpty()) return Avgjørelse.Endring

        val forrigePeriode = arvede.lastOrNull()

        // Aller første rettighetsperiode som er vurdert - ingenting å sammenligne med. Avgjørelsen reflekterer
        // om denne (første) behandlingen i det hele tatt innvilger rett, selv om en senere periode i samme
        // sending går tilbake til ingen rett (f.eks. innvilget med en innebygd fremtidig stans).
        if (forrigePeriode == null) {
            return if (nye.any { it.harRett }) Avgjørelse.Innvilgelse else Avgjørelse.Avslag
        }

        // Den kronologisk siste perioden er den reelle, gjeldende statusen - uavhengig av om den er ny eller arvet
        val gjeldendePeriode = perioder.last()

        // Hvis den gjeldende perioden er arvet (ikke ny), er de nye periodene i realiteten tilbakedaterte og
        // fullstendig overstyrt av en allerede kjent, senere periode som ikke er endret nå. Da har den gjeldende
        // statusen ikke endret seg, uansett hva de nye periodene selv sier - altså bare en Endring.
        if (!gjeldendePeriode.endret) return Avgjørelse.Endring

        return when {
            // Hadde rett fra før, men ender nå uten rett fordi den ordinære stønadsperioden er endelig
            // brukt opp (§ 4-15) - i motsetning til en vanlig Stans kan denne retten ikke gjenopptas.
            forrigePeriode.harRett && !gjeldendePeriode.harRett && ordinærKvoteErOppbrukt(opplysninger) -> Avgjørelse.Opphør

            // Hadde rett fra før, men ender nå uten rett
            forrigePeriode.harRett && !gjeldendePeriode.harRett -> Avgjørelse.Stans

            // Hadde rett fra før, og har fortsatt rett - men med et reelt opphold mellom periodene
            forrigePeriode.harRett && harReeltOppholdEtter(perioder, forrigePeriode) -> Avgjørelse.Gjenopptak

            // Hadde rett fra før, og har fortsatt rett uten opphold
            forrigePeriode.harRett -> Avgjørelse.Endring

            // Hadde ikke rett fra før, men får ny rett
            gjeldendePeriode.harRett -> Avgjørelse.Gjenopptak

            // Hadde ikke rett fra før, og har fortsatt ikke rett
            opplysninger.kunEgne.har(Rettighetstype.skalGjenopptakVurderes) -> Avgjørelse.Avslag

            else -> Avgjørelse.Stans
        }
    }

    // Leser antall gjenstående dager direkte, uavhengig av harLøpendeRett-tidslinjen, for å avgjøre om
    // stansen skyldes at den ordinære stønadsperioden faktisk er tom - ikke bare et vilkår som slår ut.
    private fun ordinærKvoteErOppbrukt(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.finnAlle(Beregning.gjenståendeDager).lastOrNull()?.verdi == 0

    private fun harReeltOppholdEtter(
        perioder: List<Rettighetsperiode>,
        forrigePeriode: Rettighetsperiode,
    ): Boolean {
        val nestePeriode = perioder.getOrNull(perioder.indexOf(forrigePeriode) + 1) ?: return false

        // Et kalenderhull på mer enn én dag er alltid et reelt opphold. Ligger periodene kant-i-kant (ingen
        // manglende dager) er det bare et reelt opphold dersom den nye perioden faktisk opphever en tidligere
        // kjent stans (se Rettighetsperiode.opphevetStans) - ellers er det bare en ren videreføring inn i
        // tidligere uvurdert tid, og ikke noe opphold.
        val dagerMellom = ChronoUnit.DAYS.between(forrigePeriode.tilOgMed, nestePeriode.fraOgMed)
        return dagerMellom > 1 || nestePeriode.opphevetStans
    }
}
