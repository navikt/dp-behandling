package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Vilkår
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.oppslag
import java.time.LocalDate

object Aldersvilkår : Vilkår<Boolean>("Aldersvilkår") {
    val virkningsdato = opplysning<LocalDate>("Virkningsdato")
    val fødselsdato = opplysning<LocalDate>("Fødselsdato")
    val aldersgrense =
        opplysning<Int>("Aldersgrense")
            .regel {
                oppslag(virkningsdato) { 67 }
            }
    val datoSøkerNårMaksAlder =
        opplysning<LocalDate>("Dato søker når maks alder")
            .regel {
                leggTilÅr(
                    fødselsdato,
                    aldersgrense,
                )
            }
    val sisteMuligeDagBrukerOppfyllerAlderskrav =
        opplysning<LocalDate>("Siste mulige dag bruker kan oppfylle alderskrav")
            .regel {
                sisteDagIMåned(datoSøkerNårMaksAlder)
            }
    val vilkår =
        vilkår()
            .regel {
                førEllerLik(virkningsdato, sisteMuligeDagBrukerOppfyllerAlderskrav)
            }
}
