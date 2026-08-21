package no.nav.dagpenger.opplysning.regel.barn

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import java.time.LocalDate

class Barnetillegg(
    produserer: Opplysningstype<BarnListe>,
    val opplysningstype: Opplysningstype<BarnListe>,
    val filter: Barn.(dato: LocalDate) -> Boolean,
) : Regel<BarnListe>(produserer, listOf(opplysningstype)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): BarnListe {
        val liste = opplysninger.finnOpplysning(opplysningstype)

        val barn = liste.verdi.barn.filter { filter(it, prøvingsdato) }
        return BarnListe(liste.id, barn)
    }

    override fun toString() = "Finner hvilke barn som oppfyller kriteriene til barnetilleggg"
}

fun Opplysningstype<BarnListe>.barnetillegg(
    opplysningstype: Opplysningstype<BarnListe>,
    filter: Barn.(dato: LocalDate) -> Boolean,
) = Barnetillegg(this, opplysningstype, filter)
