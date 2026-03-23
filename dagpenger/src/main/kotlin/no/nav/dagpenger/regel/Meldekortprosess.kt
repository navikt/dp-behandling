package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.ProsessPlugin
import no.nav.dagpenger.opplysning.Prosesskontekst
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.beregning.Beregning.forbruk
import no.nav.dagpenger.regel.beregning.Beregning.forbrukt
import no.nav.dagpenger.regel.beregning.Beregning.sisteForbruksdag
import no.nav.dagpenger.regel.beregning.Beregning.sisteGjenståendeDager
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsdager
import java.time.LocalDate

class Meldekortprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(MeldekortBeregningPlugin())
        registrer(Kvotetelling())
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val innvilgelsesdato = innvilgelsesdato(opplysninger)
        val meldeperiode = meldeperiode(opplysninger)
        val førsteDagMedRett = maxOf(innvilgelsesdato, meldeperiode.fraOgMed)

        return Regelkjøring(
            regelverksdato = innvilgelsesdato,
            prøvingsperiode = Regelkjøring.Periode(start = førsteDagMedRett, endInclusive = meldeperiode.tilOgMed),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter() = emptyList<Kontrollpunkt>()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = meldeperiode(opplysninger).tilOgMed

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }

    private fun innvilgelsesdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi

    private fun meldeperiode(opplysninger: LesbarOpplysninger): Periode = opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).verdi
}

class Kvotetelling : ProsessPlugin {
    override fun regelkjøringFerdig(kontekst: Prosesskontekst) {
        val opplysninger = kontekst.opplysninger
        val innvilgetStønadsdager = opplysninger.finnOpplysning(antallStønadsdager).verdi

        val dager = opplysninger.kunEgne.finnAlle(forbruk)

        var utgangspunkt =
            opplysninger
                .finnAlle(forbrukt)
                .lastOrNull {
                    it.gyldighetsperiode.fraOgMed.isBefore(dager.first().gyldighetsperiode.fraOgMed)
                }?.verdi ?: 0

        dager.forEach {
            if (it.verdi) utgangspunkt++
            opplysninger.leggTil(Faktum(forbrukt, utgangspunkt, it.gyldighetsperiode))

            val gjenståendeDager = innvilgetStønadsdager - utgangspunkt
            // TODO: Dette må vi bygge inn ett annet sted.
            require(gjenståendeDager >= 0) { "Gjenstående dager kan ikke være negativt. Har $gjenståendeDager dager igjen" }

            opplysninger.leggTil(Faktum(Beregning.gjenståendeDager, gjenståendeDager, it.gyldighetsperiode))
        }

        // Lag en opplysning som sporer siste dag med forbruk
        // Den må ha åpen sluttdato for å kunne brukes i gjenopptak
        // Opplysning om forbruk per dag kan ikke ha åpen slutt, da overskrives de fordi alle er i "egne"
        dager.lastOrNull { it.verdi }?.let {
            val sisteForbruksdag = it.gyldighetsperiode.fraOgMed
            opplysninger.leggTil(
                Faktum(
                    Beregning.sisteForbruksdag,
                    sisteForbruksdag,
                    Gyldighetsperiode(sisteForbruksdag),
                ),
            )

            val sisteForbruk = opplysninger.finnAlle(Beregning.gjenståendeDager).last().verdi
            opplysninger.leggTil(
                Faktum(
                    Beregning.sisteGjenståendeDager,
                    sisteForbruk,
                    Gyldighetsperiode(sisteForbruksdag),
                ),
            )
        }
    }
}
