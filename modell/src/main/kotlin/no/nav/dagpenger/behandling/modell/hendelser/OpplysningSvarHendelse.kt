package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Utledning
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
            "opplysninger" to opplysninger.map { it.opplysningstype.behovId }.sorted().joinToString(", "),
        )
}

data class OpplysningSvar<T : Comparable<T>>(
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val tilstand: Tilstand,
    val kilde: Kilde,
    val gyldighetsperiode: Gyldighetsperiode? = null,
    private val utledetAv: List<UUID> = emptyList(),
    private val utledetAvRegelsett: String?,
) {
    enum class Tilstand {
        Hypotese,
        Faktum,
    }

    fun leggTil(opplysninger: Opplysninger): Opplysning<T> {
        val basertPå = utledetAv.map { opplysninger.finnOpplysning(it) }
        val utledetAvRegelsett = utledetAvRegelsett
        val utledning =
            if (basertPå.isEmpty()) {
                null
            } else {
                Utledning("innhentMed", basertPå, regelsettnavn = utledetAvRegelsett)
            }
        val gyldighetsperiode = gyldighetsperiode ?: opplysningstype.gyldighetsperiode(verdi, basertPå)

        val opplysning =
            when (tilstand) {
                Tilstand.Hypotese ->
                    Hypotese(
                        opplysningstype,
                        verdi,
                        kilde = kilde,
                        gyldighetsperiode = gyldighetsperiode,
                        utledetAv = utledning,
                    )

                Tilstand.Faktum ->
                    Faktum(
                        opplysningstype,
                        verdi,
                        kilde = kilde,
                        gyldighetsperiode = gyldighetsperiode,
                        utledetAv = utledning,
                    )
            }
        opplysninger.leggTil(opplysning)

        return opplysning
    }
}
