package no.nav.dagpenger.regelverk

import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.Rettighetstidslinje

/**
 * Lar et regelverk slå opp en persons rettighetstidslinje uten å måtte avhenge av mediator sitt
 * persondepot (som ville krevd å laste hele personaggregatet). Brukes typisk av behovsløsere som
 * svarer ut rettighetsstatus for et annet regelverk enn sitt eget.
 */
fun interface RettighetstidslinjeOppslag {
    fun hent(ident: Ident): Rettighetstidslinje
}
