package no.nav.dagpenger.ferietillegg

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.ferietillegg.Avklaringspunkter.KontrollFerietillegg
import no.nav.dagpenger.ferietillegg.Behov.AntallDagerForbrukt
import no.nav.dagpenger.ferietillegg.Behov.OpptjeningsårFerietillegg
import no.nav.dagpenger.ferietillegg.OpplysningsTyper.antallDagerForbrukId
import no.nav.dagpenger.ferietillegg.OpplysningsTyper.ferietilleggTerskelId
import no.nav.dagpenger.ferietillegg.OpplysningsTyper.harKravPåFerietilleggId
import no.nav.dagpenger.ferietillegg.OpplysningsTyper.årSomSkalBeregnesId
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import no.nav.dagpenger.opplysning.regel.tomRegel

object KravPåFerietillegg {
    val harKravpåFerietillegg = boolsk(harKravPåFerietilleggId, "Har krav på ferietillegg")
    val antallDagerForbruk = heltall(antallDagerForbrukId, "Antall dager forbrukte i opptjeningsåret", behovId = AntallDagerForbrukt)
    val ferietilleggTerskel = heltall(ferietilleggTerskelId, "Antall dager som må være forbrukt for at man skal ha krav på ferietillegg")
    val åretDetSkalBeregnesFerietilleggFor =
        heltall(årSomSkalBeregnesId, "Året som det skal beregnes ferietillegg for", behovId = OpptjeningsårFerietillegg)

    const val MIN_ANTALL_DAGER_FORBRUKT_FOR_RETT = 8 * 5 + 1

    // Det kan ha vært omgjøring som har ført til feilutbetaling eller etterbetaling. Vi må kunne gjøre en omgjøring av forrige ferietillegg
    val regelsett =
        vilkår(
            // finn riktig hjemmel
            folketrygden.hjemmel(4, 14, "Ferietillegg", "Ferietillegg"),
        ) {
            // sende ut behov for å finne antall forbrukte dager i et år
            // sende inn år eller hente alle?
            regel(åretDetSkalBeregnesFerietilleggFor) { tomRegel }
            regel(antallDagerForbruk) { innhentMed(åretDetSkalBeregnesFerietilleggFor) }
            regel(ferietilleggTerskel) { somUtgangspunkt(MIN_ANTALL_DAGER_FORBRUKT_FOR_RETT) }

            utfall(harKravpåFerietillegg) { størreEnnEllerLik(antallDagerForbruk, ferietilleggTerskel) }
        }

    val FerietilleggKontroll =
        Kontrollpunkt(KontrollFerietillegg) {
            if (it.har(harKravpåFerietillegg)) {
                if (it.erSann(harKravpåFerietillegg)) {
                    return@Kontrollpunkt true
                }
            }

            return@Kontrollpunkt false
        }
}
