package no.nav.dagpenger.regel.regelsett.vilkår

import no.nav.dagpenger.opplysning.Forbrukstype
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Tildelingsgrunnlag
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.somUtgangspunkt
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.kravPåDagpenger
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.dagerIUka

object Sanksjonsperiode {
    val harSanksjon = Opplysningstype.boolsk(OpplysningsTyper.harSanksjonId, "Er ilagt sanskjonsperiode ved selvforskyldt arbeidsløshet")

    val antallSanksjonsuker =
        Opplysningstype.heltall(OpplysningsTyper.antallSanksjonsukerId, "Antall uker med sanksjon", enhet = Enhet.Uker)
    val antallSanksjonsdager =
        Opplysningstype.heltall(
            OpplysningsTyper.antallSanksjonsdagerId,
            "Antall dager med sanksjon",
            enhet = Enhet.Dager,
            synlig = aldriSynlig,
        )

    val regelsett =
        vilkår(folketrygden.hjemmel(4, 10, "Sanksjonsperiode ved selvforskyldt arbeidsløshet", "Sanksjonsperiode")) {
            skalVurderes { kravPåDagpenger(it) }

            regel(harSanksjon) { somUtgangspunkt(false) }
            regel(antallSanksjonsuker) { somUtgangspunkt(18) }
            regel(antallSanksjonsdager) { multiplikasjon(antallSanksjonsuker, dagerIUka) }

            kvote(
                KvoteDefinisjon(
                    hjemmel = hjemmel,
                    tildelingsgrunnlag = Tildelingsgrunnlag(antallSanksjonsdager, harSanksjon),
                    tellesNår = Beregning.erBortfallsdag,
                    forbruksteller = Beregning.forbruktSanksjonsdager,
                    gjenstående = Beregning.gjenståendeSanksjonsdager,
                    sisteForbruk = Beregning.sisteSanksjonsdagMedForbruk,
                    sisteGjenstående = Beregning.sisteGjenståendeSanksjonsdager,
                    forbrukstype = Forbrukstype.Bortfall,
                ),
            )

            ønsketResultat(antallSanksjonsdager)

            påvirkerResultat { it.erSann(harSanksjon) }
        }
}
