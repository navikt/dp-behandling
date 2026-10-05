package no.nav.dagpenger.regel.prosess

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.IKontrollpunkt
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.TreMeldePerioderUtentilstrekkeligTapAvArbeidstid
import java.time.LocalDate

class Meldekortprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(AlderskravPlugin())
        registrer(RettighetsperiodePlugin(regelverk))
        registrer(MeldekortBeregningPlugin(regelverk.kvoter()))
        registrer(TaptArbeidstidStans())
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val meldeperiode = meldeperiode(opplysninger)

        val rettighetsperioder = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett).filter { it.verdi }.map { it.gyldighetsperiode }
        val regelverksdato = rettighetsperioder.minOf { it.fraOgMed }

        val førsteMeldedagMedRett =
            rettighetsperioder.firstOrNull { it.overlapper(meldeperiode) }?.fraOgMed
                ?: meldeperiode.fraOgMed

        logger.info {
            "Meldeperiode: $meldeperiode, førsteMeldedagMedRett: $førsteMeldedagMedRett, tilOgMed: ${meldeperiode.tilOgMed}, innvilgelsesdato: $regelverksdato"
        }

        return Regelkjøring(
            regelverksdato = regelverksdato,
            prøvingsperiode = Regelkjøring.Periode(start = førsteMeldedagMedRett, endInclusive = meldeperiode.tilOgMed),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter(): List<IKontrollpunkt> =
        listOf(
            Alderskrav.StansAlderKontroll,
            // BarnFyller18ÅrKontroll,
            TreMeldePerioderUtentilstrekkeligTapAvArbeidstid.OverTerskelKontroll,
        )

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = meldeperiode(opplysninger).tilOgMed

    private fun meldeperiode(opplysninger: LesbarOpplysninger): Gyldighetsperiode =
        opplysninger.kunEgne.finnOpplysning(Beregning.meldeperiode).gyldighetsperiode

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}
