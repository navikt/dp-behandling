package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål.Bruker
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.ingenAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.Behov.Lønnsgaranti
import no.nav.dagpenger.regel.Behov.Ordinær
import no.nav.dagpenger.regel.Behov.Permittert
import no.nav.dagpenger.regel.Behov.PermittertFiskeforedling
import no.nav.dagpenger.regel.OpplysningsTyper.ErReellArbeidssøkerVurdertId
import no.nav.dagpenger.regel.OpplysningsTyper.HarRettTilOrdinærId
import no.nav.dagpenger.regel.OpplysningsTyper.IngenArbeidId
import no.nav.dagpenger.regel.OpplysningsTyper.LønnsgarantiId
import no.nav.dagpenger.regel.OpplysningsTyper.OrdinærId
import no.nav.dagpenger.regel.OpplysningsTyper.PermittertFiskeforedlingId
import no.nav.dagpenger.regel.OpplysningsTyper.PermittertId
import no.nav.dagpenger.regel.OpplysningsTyper.RettighetstypeId
import no.nav.dagpenger.regel.OpplysningsTyper.SkalVernepliktVurderesId
import no.nav.dagpenger.regel.Rettighetstype.erReellArbeidssøkerVurdert
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.Verneplikt.avtjentVerneplikt

object Rettighetstype {
    val erPermittert = boolsk(PermittertId, "Bruker er permittert", Bruker, behovId = Permittert)
    private val ordinærArbeid = boolsk(OrdinærId, beskrivelse = "Har rett til ordinære dagpenger gjennom arbeidsforhold", behovId = Ordinær)
    private val lønnsgaranti =
        boolsk(LønnsgarantiId, beskrivelse = "Forskutterte lønnsgarantimidler i form av dagpenger", behovId = Lønnsgaranti)
    val permitteringFiskeforedling =
        boolsk(
            PermittertFiskeforedlingId,
            "Permittert fra fiskeindustrien",
            Bruker,
            behovId = PermittertFiskeforedling,
        )
    val erReellArbeidssøkerVurdert: Opplysningstype<Boolean> =
        boolsk(
            ErReellArbeidssøkerVurdertId,
            "Kravet til reell arbeidssøker er vurdert",
            synlig = { !kravPåDagpenger(it) || !it.erSann(erReellArbeidssøkerVurdert) },
        )

    val skalVernepliktVurderes: Opplysningstype<Boolean> =
        boolsk(
            SkalVernepliktVurderesId,
            "Skal kravet til verneplikt vurderes",
        )

    private val ordinær = boolsk(HarRettTilOrdinærId, "Ordinære dagpenger")
    private val ingenArbeid = boolsk(IngenArbeidId, "Har rett til ordinære dagpenger uten arbeidsforhold", synlig = aldriSynlig)

    val rettighetstype = boolsk(RettighetstypeId, beskrivelse = "Rettighetstype", behovId = "Rettighetstype")

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(0, 0, "Rettighetstype", "Rettighetstype"),
        ) {
            skalVurderes { it.oppfyller(kravTilAlder) }

            regel(erPermittert) { innhentMed(søknadIdOpplysningstype) }
            regel(ordinærArbeid) { innhentMed(søknadIdOpplysningstype) }
            regel(lønnsgaranti) { innhentMed(søknadIdOpplysningstype) }
            regel(permitteringFiskeforedling) { innhentMed(søknadIdOpplysningstype) }

            regel(ingenArbeid) { ingenAv(ordinærArbeid, erPermittert, lønnsgaranti, permitteringFiskeforedling) }
            regel(ordinær) { enAv(ordinærArbeid, ingenArbeid) }

            regel(rettighetstype) { enAv(ordinær, erPermittert, lønnsgaranti, permitteringFiskeforedling) }

            regel(erReellArbeidssøkerVurdert) { somUtgangspunkt(true) }
            regel(skalVernepliktVurderes) { erSann(avtjentVerneplikt) }

            ønsketResultat(rettighetstype, erReellArbeidssøkerVurdert, skalVernepliktVurderes)
        }

    val ManglerReellArbeidssøkerKontroll =
        Kontrollpunkt(sjekker = Avklaringspunkter.ManglerReellArbeidssøker) { opplysninger ->
            kravPåDagpenger(opplysninger) && !opplysninger.erSann(erReellArbeidssøkerVurdert)
        }
}
