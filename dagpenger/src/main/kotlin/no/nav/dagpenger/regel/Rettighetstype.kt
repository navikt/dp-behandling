package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningsformål.Bruker
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.ingenAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.regel.Behov.Lønnsgaranti
import no.nav.dagpenger.regel.Behov.Ordinær
import no.nav.dagpenger.regel.Behov.Permittert
import no.nav.dagpenger.regel.Behov.PermittertFiskeforedling
import no.nav.dagpenger.regel.OpplysningsTyper.HarRettTilOrdinærId
import no.nav.dagpenger.regel.OpplysningsTyper.IngenArbeidId
import no.nav.dagpenger.regel.OpplysningsTyper.LønnsgarantiId
import no.nav.dagpenger.regel.OpplysningsTyper.OrdinærId
import no.nav.dagpenger.regel.OpplysningsTyper.PermittertFiskeforedlingId
import no.nav.dagpenger.regel.OpplysningsTyper.PermittertId
import no.nav.dagpenger.regel.OpplysningsTyper.RettighetstypeId

object Rettighetstype {
    val erPermittert = boolsk(PermittertId, "Bruker er permittert", Bruker, behovId = Permittert)
    private val ordinærArbeid = boolsk(OrdinærId, beskrivelse = "Har rett til ordinære dagpenger gjennom arbeidsforhold", behovId = Ordinær)
    private val lønnsgaranti = boolsk(LønnsgarantiId, beskrivelse = "Har rett til dagpenger etter konkurs", behovId = Lønnsgaranti)
    val permitteringFiskeforedling =
        boolsk(
            PermittertFiskeforedlingId,
            beskrivelse = "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
            behovId = PermittertFiskeforedling,
        )

    private val ordinær = boolsk(HarRettTilOrdinærId, "Har rett til ordinære dagpenger")
    private val ingenArbeid = boolsk(IngenArbeidId, "Har rett til ordinære dagpenger uten arbeidsforhold")

    val rettighetstype = boolsk(RettighetstypeId, beskrivelse = "Rettighetstype", behovId = "Rettighetstype")

    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(0, 0, "Rettighetstype", "Rettighetstype"),
        ) {
            regel(erPermittert) { innhentes }
            regel(ordinærArbeid) { innhentes }
            regel(lønnsgaranti) { innhentes }
            regel(permitteringFiskeforedling) { innhentes }

            regel(ingenArbeid) { ingenAv(ordinærArbeid, erPermittert, lønnsgaranti, permitteringFiskeforedling) }
            regel(ordinær) { enAv(ordinærArbeid, ingenArbeid) }

            regel(rettighetstype) { enAv(ordinær, erPermittert, lønnsgaranti, permitteringFiskeforedling) }

            ønsketResultat(rettighetstype)
        }
}
