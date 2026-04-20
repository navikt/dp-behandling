package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.regel.størreEnn
import no.nav.dagpenger.opplysning.regel.tomRegel
import no.nav.dagpenger.regel.Behov.AntallDagerForbukt
import no.nav.dagpenger.regel.OpplysningsTyper.antallDagerForbrukId
import no.nav.dagpenger.regel.OpplysningsTyper.ferietilleggTerskelId
import no.nav.dagpenger.regel.OpplysningsTyper.harKravPåFerietilleggId
import no.nav.dagpenger.regel.OpplysningsTyper.årSomSkalBeregnesId

object KravPåFerietillegg {
    val harKravpåFerietillegg = boolsk(harKravPåFerietilleggId, "Har krav på ferietillegg")
    val antallDagerForbruk = heltall(antallDagerForbrukId, "Antall dager forbrukte i opptjeningsåret", behovId = AntallDagerForbukt)
    val ferietilleggTerskel = heltall(ferietilleggTerskelId, "Antall dager som må være forbrukt for at man skal ha krav på ferietillegg")
    val åretDetSkalBeregnesFerietilleggFor = heltall(årSomSkalBeregnesId, "Året som det skal beregnes ferietillegg for")

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
            regel(ferietilleggTerskel) { somUtgangspunkt(8 * 5) }

            utfall(harKravpåFerietillegg) { størreEnn(antallDagerForbruk, ferietilleggTerskel) }
        }

    val FerietilleggKontroll =
        Kontrollpunkt(Avklaringspunkter.KontrollFerietillegg) {
            if (it.har(harKravpåFerietillegg)) {
                if (it.erSann(harKravpåFerietillegg)) {
                    return@Kontrollpunkt true
                }
            }

            return@Kontrollpunkt false
        }
}
