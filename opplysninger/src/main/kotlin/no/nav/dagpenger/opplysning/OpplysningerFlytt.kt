package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter
import no.nav.dagpenger.uuid.UUIDv7

/**
 * Egne opplysninger som skal følge med når en behandling flyttes til en ny behandling.
 *
 * Avledede opplysninger som er utledet av noe annet enn egne opplysninger, mister grunnlaget sitt og droppes
 * (transitivt). Resten kopieres med ny id og uten kobling til den gamle behandlingens opplysninger.
 */
fun LesbarOpplysninger.egneForFlytting(): List<Opplysning<*>> {
    val beholdt = somListe(Filter.Egne).toMutableSet()
    do {
        val mistetGrunnlag = beholdt.filter { opplysning -> opplysning.utledetAv?.opplysninger?.any { it !in beholdt } == true }
        beholdt.removeAll(mistetGrunnlag.toSet())
    } while (mistetGrunnlag.isNotEmpty())

    val kopier = mutableMapOf<java.util.UUID, Opplysning<*>>()

    fun kopier(opplysning: Opplysning<*>): Opplysning<*> =
        kopier.getOrPut(opplysning.id) {
            val utledetAv = opplysning.utledetAv?.let { it.copy(opplysninger = it.opplysninger.map(::kopier)) }
            @Suppress("UNCHECKED_CAST")
            (opplysning as Opplysning<Any>).kopiMedNyId(utledetAv)
        }

    return beholdt.sortedBy { it.id }.map(::kopier)
}

private fun Opplysning<Any>.kopiMedNyId(utledetAv: Utledning?): Opplysning<Any> =
    when (this) {
        is Faktum -> Faktum(UUIDv7.ny(), opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet)
        is Hypotese -> Hypotese(UUIDv7.ny(), opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet)
        is Tombstone -> throw IllegalStateException("Tombstones skal ikke flyttes")
    }
