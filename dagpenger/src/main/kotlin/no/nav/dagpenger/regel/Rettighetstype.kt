package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
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
    private val ordinærArbeid = boolsk(OrdinærId, beskrivelse = "Har rett til ordinære dagpenger gjennom arbeidsforhold", behovId = Ordinær)
    private val permittering = boolsk(PermittertId, beskrivelse = "Har rett til dagpenger under permittering", behovId = Permittert)
    private val lønnsgaranti = boolsk(LønnsgarantiId, beskrivelse = "Har rett til dagpenger etter konkurs", behovId = Lønnsgaranti)
    private val permitteringFiskeforedling =
        boolsk(
            PermittertFiskeforedlingId,
            beskrivelse = "Har rett til dagpenger under permittering i fiskeforedlingsindustri",
            behovId = PermittertFiskeforedling,
        )

    private val ordinær = boolsk(HarRettTilOrdinærId, "Har rett til ordinære dagpenger")
    private val ingenArbeid = boolsk(IngenArbeidId, "Har rett til ordinære dagpenger uten arbeidsforhold")

    val rettighetstype = boolsk(RettighetstypeId, beskrivelse = "Rettighetstype", behovId = "Rettighetstype")

    val regelsett =
        Regelsett(
            folketrygden.hjemmel(0, 0, "Rettighetstype", "Rettighetstype"),
            RegelsettType.Fastsettelse,
        ) {
            regel(ordinærArbeid) { innhentes }
            regel(permittering) { innhentes }
            regel(lønnsgaranti) { innhentes }
            regel(permitteringFiskeforedling) { innhentes }

            regel(ingenArbeid) { ingenAv(ordinærArbeid, permittering, lønnsgaranti, permitteringFiskeforedling) }
            regel(ordinær) { enAv(ordinærArbeid, ingenArbeid) }

            regel(rettighetstype) { enAv(ordinær, permittering, lønnsgaranti, permitteringFiskeforedling) }
        }
}
