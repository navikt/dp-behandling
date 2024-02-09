package no.nav.dagpenger.opplysning

interface Klassifiserbart {
    fun er(type: Opplysningstype<*>): Boolean
}

class Opplysningstype<T : Comparable<T>>(
    val navn: String,
    private val parent: Opplysningstype<T>? = null,
    private val child: MutableSet<Opplysningstype<*>> = mutableSetOf(),
) : Klassifiserbart {
    init {
        parent?.child?.add(this)
    }

    override infix fun er(type: Opplysningstype<*>): Boolean {
        return navn == type.navn || parent?.er(type) ?: false
    }

    override fun toString(): String {
        // TODO: Det er noe muffens med hierarki og data class
        return "Opplysningstype(navn='$navn', parent=${parent?.navn}, child=${child.size})"
    }

    override fun equals(other: Any?): Boolean = other is Opplysningstype<*> && other.navn == this.navn

    override fun hashCode() = navn.hashCode() * 31
}