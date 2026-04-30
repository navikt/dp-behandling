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
                        appendLine(tekst)
                        appendLine()
                    }
                }
            }
        }.trimEnd() + "\n"
}
