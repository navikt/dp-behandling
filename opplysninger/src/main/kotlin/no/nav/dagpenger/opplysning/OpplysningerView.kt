package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.util.UUID

/**
 * Et lett, kopieringsfritt view over [Opplysninger] som filtrerer på dato og/eller eierskap.
 *
 * I stedet for å lage en ny [Opplysninger]-instans (med kopiering av lister), holder viewet
 * bare en referanse til den opprinnelige instansen og anvender filtre ved oppslag.
 *
 * Views er komponebare: `forDato(dato)` på et view med `bareEgne=true` gir et nytt view
 * med begge filtre aktive.
 */
internal class OpplysningerView(
    private val source: Opplysninger,
    private val gjelderFor: LocalDate? = null,
    private val bareEgne: Boolean = false,
) : LesbarOpplysninger {
    override val id: UUID get() = source.id

    override val kunEgne: LesbarOpplysninger
        get() = OpplysningerView(source, gjelderFor, bareEgne = true)

    override fun <T : Any> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> =
        finnNullableOpplysning(opplysningstype)
            ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig")

    override fun <T : Any> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>? =
        source.finnNullableOpplysningMedFiltre(opplysningstype, gjelderFor, bareEgne)

    override fun finnOpplysning(opplysningId: UUID): Opplysning<*> {
        val opplysning =
            hentBaseListe().lastOrNull { it.id == opplysningId }
                ?: throw OpplysningIkkeFunnetException("Har ikke opplysning med id=$opplysningId")
        return opplysning
    }

    override fun <T : Any> har(opplysningstype: Opplysningstype<T>): Boolean = finnNullableOpplysning(opplysningstype) != null

    override fun finnFlere(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>> =
        opplysningstyper.mapNotNull { type ->
            hentBaseListe().lastOrNull { it.er(type) }
        }

    override fun <T : Any> finnAlle(opplysningstyper: List<Opplysningstype<T>>): List<Opplysning<T>> =
        opplysningstyper.flatMap { type -> finnAlle(type) }

    override fun <T : Any> finnAlle(opplysningstype: Opplysningstype<T>): List<Opplysning<T>> =
        hentBaseListe()
            .filter { it.er(opplysningstype) }
            .filterIsInstance<Opplysning<T>>()

    override fun forDato(gjelderFor: LocalDate): LesbarOpplysninger = OpplysningerView(source, gjelderFor, bareEgne)

    override fun erSann(opplysningstype: Opplysningstype<Boolean>): Boolean = har(opplysningstype) && finnOpplysning(opplysningstype).verdi

    override fun erErstattet(opplysninger: List<Opplysning<*>>): Boolean {
        // Viktig: Bruk bare opplysninger som er gyldige for dette viewet, slik at
        // erstatninger utenfor viewets datofilter ikke påvirker resultatet.
        val filtrert = hentBaseListe()
        val erstattetIder = filtrert.mapNotNull { it.erstatter }.map { it.id }.toSet()
        return opplysninger.any { it.id in erstattetIder }
    }

    override fun erErstattet(opplysningId: UUID): Boolean {
        val filtrert = hentBaseListe()
        val erstattetIder = filtrert.mapNotNull { it.erstatter }.map { it.id }.toSet()
        return opplysningId in erstattetIder
    }

    override fun somListe(filter: LesbarOpplysninger.Filter): List<Opplysning<*>> {
        val effektivFilter =
            if (bareEgne) LesbarOpplysninger.Filter.Egne else filter
        val baseListe = source.somListe(effektivFilter)
        return if (gjelderFor != null) {
            baseListe.filter { it.gyldighetsperiode.inneholder(gjelderFor) }
        } else {
            baseListe
        }
    }

    private fun hentBaseListe(): List<Opplysning<*>> {
        val base = source.hentOpplysninger(bareEgne)
        return if (gjelderFor != null) {
            base.filter { it.gyldighetsperiode.inneholder(gjelderFor) }
        } else {
            base
        }
    }
}
