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
    fun `henter ut hvilken hvilke opplysninger som brukes`() {
        val markdown =
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
            >
            >|UUID|Beskrivelse|Behov|Logisk datatype|Datatype|
            >|--|---|---|---|---|
            ${
                opplysninger.sortedBy { it.id.uuid }.joinToString("\n") {
                    ">|${it.id.uuid}|${it.navn}|${it.behovId}|${it.datatype}|${it.datatype.klasse.simpleName}|"
                }
            }
            """.trimMargin(">")

        skriv(markdown)
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
