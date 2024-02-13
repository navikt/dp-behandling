package no.nav.dagpenger.opplysning

interface Klassifiserbart {
    fun er(type: Opplysningstype<*>): Boolean
}

fun String.id(id: String) = OpplysningId(id, this)

data class OpplysningId(val id: String, val beskrivelse: String)

class Opplysningstype<T : Comparable<T>>(
    private val opplysningId: OpplysningId,
    private val parent: Opplysningstype<T>? = null,
    private val child: MutableSet<Opplysningstype<*>> = mutableSetOf(),
) : Klassifiserbart {
    constructor(navn: String, parent: Opplysningstype<T>? = null) : this(OpplysningId(navn, navn), parent)

    companion object {
        val typer = mutableListOf<Opplysningstype<*>>()
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
