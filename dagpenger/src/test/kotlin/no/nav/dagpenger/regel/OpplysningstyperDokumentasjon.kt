package no.nav.dagpenger.regel

import com.spun.util.persistence.Loader
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class OpplysningstyperDokumentasjon {
    val opplysninger = RegelverkDagpenger.produserer + fagsakIdOpplysningstype

    @Test
    fun `opplysninger gruppert etter regelsett`() {
        val regelverk =
            RegelverkDagpenger.regelsett.sortedBy { it.hjemmel.paragraf }.associateWith {
                it.produserer
            }
        val markdown =
            StringBuilder(
                """
            ># Dokumentasjon av opplysninger
            >
            >Dette er opplysninger som blir brukt av regelverket. 
            >
            > UUID og datatype er en unik identifikator for opplysningstypen. Den skal _ALDRI_ endres. Beskrivelse og behovId kan endres. 
            > 
            > For nye opplysningtyper, generer en ny UUID og legg til.
            > 
            > Generering av UUID kan gjøres med UUIDv7.ny() i Kotlin
            >""".trimMargin(">"),
            )

        markdown.appendLine("## Regelsett")
        regelverk.forEach { (regelsett, opplysninger) ->
            markdown.appendLine("### ${regelsett.hjemmel}")
            markdown.appendLine("*Type:* ${regelsett.type}")

            if (regelsett.avklaringer.isNotEmpty()) {
                markdown.appendLine("#### Avklaringer")

                regelsett.avklaringer.forEach {
                    markdown.appendLine("- ${it.kode} - ${it.tittel}")
                }
            }

            if (regelsett.avhengerAv.isNotEmpty()) {
                val andreRegeverk = regelverk.filter { (_, produserer) -> produserer.any { it in regelsett.avhengerAv } }

                markdown.appendLine("#### Avhenger på data fra")
                andreRegeverk.forEach { (avhengighet, _) ->
                    markdown.appendLine("- ${avhengighet.hjemmel}")
                }
            }

            markdown.appendLine(
                """
                >#### Opplysninger
                >|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|
                >|---|---|---|---|---|
                """.trimMargin(">"),
            )

            opplysninger.sortedBy { it.id.uuid }.forEach {
                val behovId = if (it.navn == it.behovId) "" else it.behovId

                markdown.appendLine(
                    "|${it.id.uuid}|${it.navn}|${it.datatype}|${it.datatype.klasse.simpleName}|$behovId|",
                )
            }
        }

        skriv(markdown.toString())
    }

    private companion object {
        val path = "${Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")}/docs/"
        val options = Options().forFile().withExtension(".md")
    }

    private fun skriv(behov: String) {
        Approvals.namerCreater = Loader { NamerWrapper({ "opplysninger" }, { path }) }
        Approvals.verify(behov, options)
    }
}
