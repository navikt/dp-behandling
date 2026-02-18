package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.GyldighetsperiodeStrategi
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.opplysning.verdier.Ulid
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import java.time.LocalDate
import java.util.UUID

interface Klassifiserbart {
    fun er(type: Opplysningstype<*>): Boolean
}

enum class Opplysningsformål {
    Legacy,
    Bruker,
    Register,
    Regel,
}

enum class OpplysningstypeKategori {
    Prosess,
    Materiell,
}

class Opplysningstype<T : Comparable<T>>(
    val id: Id<T>,
    val navn: String,
    val behovId: String,
    val formål: Opplysningsformål,
    val synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
    private val gyldighetsperiodeStrategi: GyldighetsperiodeStrategi<T> = GyldighetsperiodeStrategi.minsteMulige(),
    val enhet: Enhet? = null,
    val utgåtteBehovId: Set<String> = emptySet(),
    /** Om opplysninger av denne typen skal arves til neste behandling i kjeden. Default true. */
    val opplysningstypeKategori: OpplysningstypeKategori = OpplysningstypeKategori.Materiell,
) : Klassifiserbart {
    val datatype = id.datatype

    init {
        definerteTyper.add(this)
    }

    data class Id<T : Comparable<T>>(
        val uuid: UUID,
        val datatype: Datatype<T>,
    )

    fun gyldighetsperiode(
        produkt: T,
        basertPå: List<Opplysning<*>>,
    ): Gyldighetsperiode = gyldighetsperiodeStrategi.gyldighetsperiode(produkt, basertPå)

    companion object {
        val definerteTyper = mutableSetOf<Opplysningstype<*>>()

        val alltidSynlig: Opplysningssjekk = { true }
        val aldriSynlig: Opplysningssjekk = { false }

        fun periode(
            id: Id<Periode>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<Periode> = som(id, beskrivelse, formål, synlig, behovId, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun heltall(
            id: Id<Int>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<Int> = som(id, beskrivelse, formål, synlig, behovId, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun boolsk(
            id: Id<Boolean>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            gyldighetsperiode: GyldighetsperiodeStrategi<Boolean> = GyldighetsperiodeStrategi.minsteMulige(),
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
            opplysningstypeKategori: OpplysningstypeKategori = OpplysningstypeKategori.Materiell,
        ): Opplysningstype<Boolean> =
            som(
                id,
                beskrivelse,
                formål,
                synlig,
                behovId,
                gyldighetsperiode,
                enhet = enhet,
                utgåtteBehovId = utgåtteBehovId,
                opplysningstypeKategori = opplysningstypeKategori,
            )

        fun dato(
            id: Id<LocalDate>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            gyldighetsperiode: GyldighetsperiodeStrategi<LocalDate> = GyldighetsperiodeStrategi.minsteMulige(),
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<LocalDate> =
            som(id, beskrivelse, formål, synlig, behovId, gyldighetsperiode, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun ulid(
            id: Id<Ulid>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<Ulid> = som(id, beskrivelse, formål, synlig, behovId, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun inntekt(
            id: Id<Inntekt>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<Inntekt> = som(id, beskrivelse, formål, synlig, behovId, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun desimaltall(
            id: Id<Double>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<Double> = som(id, beskrivelse, formål, synlig, behovId, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun tekst(
            id: Id<String>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<String> = som(id, beskrivelse, formål, synlig, behovId, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun beløp(
            id: Id<Beløp>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<Beløp> = som(id, beskrivelse, formål, synlig, behovId, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun barn(
            id: Id<BarnListe>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
        ): Opplysningstype<BarnListe> = som(id, beskrivelse, formål, synlig, behovId, enhet = enhet, utgåtteBehovId = utgåtteBehovId)

        fun <T : Comparable<T>> som(
            id: Id<T>,
            beskrivelse: String,
            formål: Opplysningsformål = Opplysningsformål.Regel,
            synlig: (LesbarOpplysninger) -> Boolean = alltidSynlig,
            behovId: String = beskrivelse,
            gyldighetsperiodeStrategi: GyldighetsperiodeStrategi<T> = GyldighetsperiodeStrategi.minsteMulige(),
            enhet: Enhet? = null,
            utgåtteBehovId: Set<String> = emptySet(),
            opplysningstypeKategori: OpplysningstypeKategori = OpplysningstypeKategori.Materiell,
        ): Opplysningstype<T> =
            Opplysningstype(
                id,
                beskrivelse,
                behovId,
                formål,
                synlig,
                gyldighetsperiodeStrategi,
                enhet,
                utgåtteBehovId,
                opplysningstypeKategori,
            )
    }

    override infix fun er(type: Opplysningstype<*>): Boolean = id == type.id

    override fun toString() = navn

    override fun equals(other: Any?): Boolean = other is Opplysningstype<*> && other.id == this.id

    override fun hashCode() = id.hashCode() * 31
}
