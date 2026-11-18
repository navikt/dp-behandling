package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.opplysning.Behovsløserkilde
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Utledning
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.isNotEmpty
import kotlin.collections.map

class OpplysningSvarHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    val opplysninger: List<OpplysningSvar<*>>,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse {
    init {
        require(opplysninger.isNotEmpty()) { "Må ha minst én opplysning" }
    }

    override fun kontekstMap(): Map<String, String> =
        mapOf(
            "behandlingId" to behandlingId.toString(),
        )
}

sealed interface Svarresultat {
    val endrerOpplysninger: Boolean get() = this is Opprettet

    data class Opprettet(
        val opplysning: Opplysning<*>,
    ) : Svarresultat

    // Grunnlaget svaret er innhentet med er byttet ut
    data object Utdatert : Svarresultat

    // Samme innhenting er allerede besvart
    data object Duplikat : Svarresultat

    data object UtenforGrunnlaget : Svarresultat
}

// Et svar kan ikke gjelde utenfor det grunnlaget det er innhentet med gjelder (utflatet) for
private fun Gyldighetsperiode.begrensesAv(grunnlag: List<Opplysning<*>>): Gyldighetsperiode? =
    grunnlag.fold<Opplysning<*>, Gyldighetsperiode?>(this) { periode, it -> periode?.overlapp(it.gyldighetsperiode) }

data class OpplysningSvar<T : Any>(
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val tilstand: Tilstand,
    val kilde: Kilde,
    val gyldighetsperiode: Gyldighetsperiode? = null,
    private val utledetAv: List<UUID> = emptyList(),
) {
    enum class Tilstand {
        Hypotese,
        Faktum,
    }

    fun leggTil(opplysninger: Opplysninger): Svarresultat {
        if (erUtdatert(opplysninger)) return Svarresultat.Utdatert

        val basertPå = utledetAv.map { opplysninger.finnOpplysning(it) }
        val utledning =
            if (basertPå.isEmpty()) {
                null
            } else {
                Utledning("innhentMed", basertPå)
            }
        // TODO ??????????????
        val prøvingsdato = LocalDate.now()
        val begrenset = gyldighetsperiode?.begrensesAv(basertPå)
        val utenforGrunnlaget = gyldighetsperiode != null && begrenset == null
        val gyldighetsperiode = begrenset ?: gyldighetsperiode ?: opplysningstype.gyldighetsperiode(verdi, basertPå, prøvingsdato)

        val opplysning =
            when (tilstand) {
                Tilstand.Hypotese -> {
                    Hypotese(
                        opplysningstype,
                        verdi,
                        kilde = kilde,
                        gyldighetsperiode = gyldighetsperiode,
                        utledetAv = utledning,
                    )
                }

                Tilstand.Faktum -> {
                    Faktum(
                        opplysningstype,
                        verdi,
                        kilde = kilde,
                        gyldighetsperiode = gyldighetsperiode,
                        utledetAv = utledning,
                    )
                }
            }
        if (utenforGrunnlaget) return Svarresultat.UtenforGrunnlaget
        if (erDuplikat(opplysninger, opplysning)) return Svarresultat.Duplikat

        opplysninger.leggTil(opplysning)
        return Svarresultat.Opprettet(opplysning)
    }

    private fun erUtdatert(opplysninger: Opplysninger): Boolean {
        val gjeldende = opplysninger.somListe().mapTo(HashSet()) { it.id }
        return utledetAv.any { it !in gjeldende }
    }

    // Samme innhenting besvart flere ganger (f.eks. duplikate behov) skal ikke utløse ny regelkjøring
    private fun erDuplikat(
        opplysninger: Opplysninger,
        ny: Opplysning<T>,
    ): Boolean {
        if (ny.kilde !is Behovsløserkilde) return false
        return opplysninger.finnAlle(opplysningstype).any {
            it::class == ny::class &&
                it.verdi == ny.verdi &&
                it.gyldighetsperiode == ny.gyldighetsperiode &&
                it.kilde is Behovsløserkilde &&
                it.utledetAv
                    ?.opplysninger
                    ?.map { grunnlag -> grunnlag.id }
                    .orEmpty()
                    .toSet() == utledetAv.toSet()
        }
    }
}
