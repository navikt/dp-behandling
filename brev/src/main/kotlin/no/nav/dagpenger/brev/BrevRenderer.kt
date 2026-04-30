package no.nav.dagpenger.brev

/**
 * Rendrer et [Brev] til et tekstformat.
 */
fun interface BrevRenderer {
    fun render(brev: Brev): String
}

/**
 * Rendrer brev til Markdown.
 */
object MarkdownRenderer : BrevRenderer {
    override fun render(brev: Brev): String =
        buildString {
            appendLine("# ${brev.overskrift}")
            appendLine()

            for (seksjon in brev.seksjoner) {
                if (seksjon.tittel != null) {
                    appendLine("## ${seksjon.tittel}")
                    appendLine()
                }
                for (tekst in seksjon.innhold) {
                    if (tekst.isNotBlank()) {
                        appendLine(tekst)
                        appendLine()
                    }
                }
            }
        }.trimEnd() + "\n"
}

/**
 * Rendrer brev til Typst.
 */
object TypstRenderer : BrevRenderer {
    private val markdownLinkPattern = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
    private val markdownBoldPattern = Regex("""\*\*([^*]+)\*\*""")

    override fun render(brev: Brev): String =
        buildString {
            appendLine("= ${brev.overskrift}")
            appendLine()

            for (seksjon in brev.seksjoner) {
                if (seksjon.tittel != null) {
                    appendLine("== ${seksjon.tittel}")
                    appendLine()
                }
                for (tekst in seksjon.innhold) {
                    if (tekst.isNotBlank()) {
                        appendLine(tekst.tilTypst())
                        appendLine()
                    }
                }
            }
        }.trimEnd() + "\n"

    private fun String.tilTypst(): String =
        this
            .replace(markdownLinkPattern) { match ->
                val tekst = match.groupValues[1]
                val url = match.groupValues[2]
                "#link(\"$url\")[$tekst]"
            }.replace(markdownBoldPattern) { match ->
                val innhold = match.groupValues[1]
                "*$innhold*"
            }
}
