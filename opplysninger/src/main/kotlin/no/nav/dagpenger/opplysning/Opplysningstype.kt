package no.nav.dagpenger.opplysning

import java.time.LocalDate

interface Klassifiserbart {
    fun er(type: Opplysningstype<*>): Boolean
}

fun String.id(id: String) = OpplysningId(id, this)

data class OpplysningId(val id: String, val beskrivelse: String)

class Opplysningstype<T : Comparable<T>> private constructor(
    private val opplysningId: OpplysningId,
    val datatype: Datatype<T>,
    private val parent: Opplysningstype<T>? = null,
    private val child: MutableSet<Opplysningstype<*>> = mutableSetOf(),
) : Klassifiserbart {
    constructor(navn: String, datatype: Datatype<T>, parent: Opplysningstype<T>? = null) : this(OpplysningId(navn, navn), datatype, parent)

    companion object {
        val typer = mutableListOf<Opplysningstype<*>>()

        fun somHeltall(
            opplysningId: OpplysningId,
            parent: Opplysningstype<Int>? = null,
        ) = Opplysningstype(opplysningId, Heltall, parent)

        fun somHeltall(
            navn: String,
            parent: Opplysningstype<Int>? = null,
        ) = somHeltall(navn.id(navn), parent)

        fun somDesimaltall(
            opplysningId: OpplysningId,
            parent: Opplysningstype<Double>? = null,
        ) = Opplysningstype(opplysningId, Desimaltall, parent)

        fun somDesimaltall(
            navn: String,
            parent: Opplysningstype<Double>? = null,
        ) = somDesimaltall(navn.id(navn), parent)

        fun somDato(
            opplysningId: OpplysningId,
            parent: Opplysningstype<LocalDate>? = null,
        ) = Opplysningstype(opplysningId, Dato, parent)

        fun somDato(
            navn: String,
            parent: Opplysningstype<LocalDate>? = null,
        ) = somDato(navn.id(navn), parent)

        fun somBoolsk(
            opplysningId: OpplysningId,
            parent: Opplysningstype<Boolean>? = null,
        ) = Opplysningstype(opplysningId, Boolsk, parent)

        fun somBoolsk(
            navn: String,
            parent: Opplysningstype<Boolean>? = null,
        ) = somBoolsk(navn.id(navn), parent)
    }

    init {
        typer.add(this)
    }

    val id = opplysningId.id
    val navn = opplysningId.beskrivelse

    init {
        parent?.child?.add(this)
    }

    override infix fun er(type: Opplysningstype<*>): Boolean {
        return opplysningId == type.opplysningId || parent?.er(type) ?: false
    }

    override fun toString(): String {
        // TODO: Det er noe muffens med hierarki og data class
        return "Opplysningstype(navn='$navn', parent=${parent?.navn}, child=${child.size})"
    }

    override fun equals(other: Any?): Boolean = other is Opplysningstype<*> && other.opplysningId == this.opplysningId

    override fun hashCode() = navn.hashCode() * 31
}
