package no.nav.dagpenger.ferietillegg

import no.nav.dagpenger.ferietillegg.Behov.OpptjeningsBeløp
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg.harKravpåFerietillegg
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor
import no.nav.dagpenger.ferietillegg.OpplysningsTyper.ferietilleggBeløpId
import no.nav.dagpenger.ferietillegg.OpplysningsTyper.ferietilleggProsentId
import no.nav.dagpenger.ferietillegg.OpplysningsTyper.sumUtbetaltForÅrId
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.beløp
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.desimaltall
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.prosentAv
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt

object FerietilleggBeløp {
    val ferietilleggBeløp = beløp(ferietilleggBeløpId, "Beløp på ferietillegg")
    val sumUtbetaltForÅr = beløp(sumUtbetaltForÅrId, "Sum utbetalt for et år", behovId = OpptjeningsBeløp)
    val ferietilleggProsent = desimaltall(ferietilleggProsentId, "Prosent for ferietillegg")

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 14, "Ferietillegg fastsettelse", "Ferietillegg fastsettelse"),
        ) {
            skalVurderes { it.erSann(harKravpåFerietillegg) }

            // sende ut behov for å finne antall forbrukte dager i et år
            // her må vi enten sende med år, eller hente for alle år?
            regel(sumUtbetaltForÅr) { innhentMed(åretDetSkalBeregnesFerietilleggFor) }

            // husk å bruke terskel eller dele på hundre
            regel(ferietilleggProsent) { somUtgangspunkt(9.5) }
            regel(ferietilleggBeløp) { prosentAv(sumUtbetaltForÅr, ferietilleggProsent) }

            ønsketResultat(ferietilleggBeløp)
            påvirkerResultat { it.erSann(harKravpåFerietillegg) }
        }
}
