package no.nav.dagpenger.regel.regelsett.vilkår
import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål.Bruker
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.ingenAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.regel.Avklaringspunkter
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
import no.nav.dagpenger.regel.OpplysningsTyper.skalEksportVurderesId
import no.nav.dagpenger.regel.OpplysningsTyper.skalGjenopptakVurderesId
import no.nav.dagpenger.regel.kravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.kravTilAlder
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.avtjentVerneplikt

object Rettighetstype {
    val skalReellArbeidssøkerVurderes: Opplysningstype<Boolean> =
        boolsk(
            ErReellArbeidssøkerVurdertId,
            "Skal reell arbeidssøker vurderes",
            synlig = { !kravPåDagpenger(it) || !it.erSann(skalReellArbeidssøkerVurderes) },
        )

    private val ordinærArbeid = boolsk(OrdinærId, beskrivelse = "Har rett til ordinære dagpenger gjennom arbeidsforhold", behovId = Ordinær)
    private val lønnsgaranti =
        boolsk(LønnsgarantiId, beskrivelse = "Forskutterte lønnsgarantimidler i form av dagpenger", behovId = Lønnsgaranti)

    val skalPermitteringFiskeforedlingVurderes =
        boolsk(PermittertFiskeforedlingId, "Skal permittert fra fiskeindustrien vurderes", Bruker, behovId = PermittertFiskeforedling)
    val skalPermitteringVurderes = boolsk(PermittertId, "Skal permittering vurderes", Bruker, behovId = Permittert)
    val skalVernepliktVurderes = boolsk(SkalVernepliktVurderesId, "Skal verneplikt vurderes")
    val skalGjenopptakVurderes = boolsk(skalGjenopptakVurderesId, "Skal gjenopptak vurderes")
    val skalEksportVurderes = boolsk(skalEksportVurderesId, "Skal eksport vurderes")

    private val ordinær = boolsk(HarRettTilOrdinærId, "Ordinære dagpenger")
    private val ingenArbeid = boolsk(IngenArbeidId, "Har rett til ordinære dagpenger uten arbeidsforhold", synlig = aldriSynlig)

    val rettighetstype = boolsk(RettighetstypeId, beskrivelse = "Rettighetstype", behovId = "Rettighetstype")

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(0, 0, "Rettighetstype", "Rettighetstype"),
        ) {
            skalVurderes { it.oppfyller(kravTilAlder) }

            regel(skalPermitteringVurderes) { innhentMed(søknadIdOpplysningstype) }
            regel(ordinærArbeid) { innhentMed(søknadIdOpplysningstype) }
            regel(lønnsgaranti) { innhentMed(søknadIdOpplysningstype) }
            regel(skalPermitteringFiskeforedlingVurderes) { innhentMed(søknadIdOpplysningstype) }

            regel(ingenArbeid) { ingenAv(ordinærArbeid, skalPermitteringVurderes, lønnsgaranti, skalPermitteringFiskeforedlingVurderes) }
            regel(ordinær) { enAv(ordinærArbeid, ingenArbeid) }

            regel(rettighetstype) { enAv(ordinær, skalPermitteringVurderes, lønnsgaranti, skalPermitteringFiskeforedlingVurderes) }

            regel(skalReellArbeidssøkerVurderes) { somUtgangspunkt(true) }
            regel(skalVernepliktVurderes) { erSann(avtjentVerneplikt) }

            regel(skalGjenopptakVurderes) { somUtgangspunkt(false) }

            regel(skalEksportVurderes) { somUtgangspunkt(false) }

            ønsketResultat(
                rettighetstype,
                skalReellArbeidssøkerVurderes,
                skalVernepliktVurderes,
                skalGjenopptakVurderes,
                skalEksportVurderes,
            )
        }

    val ManglerReellArbeidssøkerKontroll =
        Kontrollpunkt(avklaringkode = Avklaringspunkter.ManglerReellArbeidssøker) { opplysninger ->
            kravPåDagpenger(opplysninger) && !opplysninger.erSann(skalReellArbeidssøkerVurderes)
        }
}
