package no.nav.dagpenger.dag.printer

class AlphabetIdGenerator(
    startingId: String = "A",
) {
    private var currentId = startingId

    fun getNextId(): String {
        val nextId = currentId
        currentId = incrementId(currentId)
        return nextId
    }

    private fun incrementId(id: String): String {
        if (id.isEmpty()) return "A"

        val lastIndex = id.length - 1
        val lastChar = id[lastIndex]
        return if (lastChar < 'Z') {
            id.substring(0, lastIndex) + (lastChar + 1)
        } else {
            incrementId(id.substring(0, lastIndex)) + 'A'
        }
    }
}
