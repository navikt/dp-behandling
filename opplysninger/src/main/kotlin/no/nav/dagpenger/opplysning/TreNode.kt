package no.nav.dagpenger.opplysning

data class TreNode<T>(
    val verdi: T,
    val avhengigheter: List<TreNode<T>> = emptyList(),
) {
    fun topologisk(): List<TreNode<T>> {
        // går gjennom treet breadth-first, men gjør slik at de dypeste nodene kommer først i listen
        val kø = mutableListOf(this)
        val topologisk = mutableListOf<TreNode<T>>()
        while (kø.isNotEmpty()) {
            val n = kø.removeFirst()
            topologisk.addFirst(n)
            kø.addAll(n.avhengigheter.reversed())
        }
        return topologisk
    }
}
