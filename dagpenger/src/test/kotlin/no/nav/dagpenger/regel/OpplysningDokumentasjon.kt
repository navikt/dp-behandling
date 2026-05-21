package no.nav.dagpenger.regel

import com.spun.util.persistence.Loader
import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Avklaringkode.Companion.alleAvklaringer
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.TomRegel
import no.nav.dagpenger.opplysning.regel.Utgangspunkt
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.fagsakIdOpplysningstype
import no.nav.dagpenger.regel.prosess.Manuellprosess
import no.nav.dagpenger.regel.prosess.Omgjøringsprosess
import no.nav.dagpenger.regel.prosess.Søknadsprosess
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.time.LocalDate

class OpplysningDokumentasjon {
    val opplysninger = RegelverkDagpenger.produserer + fagsakIdOpplysningstype

    @Test
    fun `opplysninger gruppert etter regelsett`() {
        val regelverk =
            RegelverkDagpenger.regelsett.sortedBy { it.hjemmel.paragraf }.associateWith {
                it.produserer
            }

        val behov = RegelverkDagpenger.regelsett.flatMap { it.behov }
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
                    markdown.appendLine("- ${it.kode} - [${it.tittel}](./avklaringer.approved.md#${tilMarkdownURL(it.tittel)})")
                }
            }

            if (regelsett.avhengerAv.isNotEmpty()) {
                val andreRegeverk = regelverk.filter { (_, produserer) -> produserer.any { it in regelsett.avhengerAv } }

                markdown.appendLine("#### Avhenger på data fra")
                andreRegeverk.forEach { (avhengighet, _) ->
                    markdown.appendLine("- [${avhengighet.hjemmel}](#${tilMarkdownURL(avhengighet.hjemmel.toString())})")
                }
            }

            markdown.appendLine(
                """
                >#### Opplysninger
                >|UUID|Beskrivelse|Logisk datatype|Datatype|Behov|Enhet|Rolle|
                >|---|---|---|---|---|---|---|
                """.trimMargin(">"),
            )

            opplysninger.sortedBy { it.id.uuid }.forEach {
                val behovId = if (it in behov) it.behovId else ""
                val enhet = if (it.enhet != null) " ${it.enhet}" else ""
                val regel = regelsett.regler(LocalDate.now()).single { regel -> regel.produserer(it) }

                var regeltype =
                    when (regel) {
                        is Ekstern -> "Ekstern"

                        is TomRegel,
                        is Utgangspunkt,
                        -> "Utgangspunkt"

                        else -> "Intern"
                    }

                regeltype = if (regelsett.ønsketInformasjon.contains(it)) "Resultat" else regeltype

                markdown.appendLine(
                    "|${it.id.uuid}|${it.navn}|${it.datatype}|${it.datatype.klasse.simpleName}|$behovId|$enhet|$regeltype",
                )
            }
        }

        skriv("opplysninger", markdown.toString())
    }

    @Test
    fun `dokumenterer hvilke opplysninger som innhentes via behov`() {
        val regler = RegelverkDagpenger.regelsett

        val behov = regler.flatMap { it.behov }

        val markdown =
            """
            ># Dokumentasjon på behov for opplysninger
            >
            >Dette er opplysninger som blir innhentet som en del av dagpengebehandlingen. De publiseres som behov på rapiden.
            >
            >|Behov|Beskrivelse|Logisk datatype|Datatype|Utgåtte behov | 
            >|---|---|---|---|---|
            ${
                behov.sortedBy { it.behovId }.joinToString("\n") {
                    ">|${it.behovId} | ${it.navn} | ${it.datatype}|${it.datatype.klasse.simpleName}|${
                        it.utgåtteBehovId.joinToString {
                            it
                        }
                    }"
                }
            }
            """.trimMargin(">")

        skriv("behov", markdown)
    }

    @Test
    fun `dokumenterer avklaringer`() {
        val avhengigheter = finnAvhengigheter()

        val markdown = StringBuilder()
        markdown.appendLine("# Avklaringer")

        // language="Markdown"
        markdown.appendLine(
            """
            |Avklaringer opprettes hvor regelmotoren er usikker på enten fakta eller riktig vei videre.
            |
            |Avklaringer opprettes av "kontrollpunkt" som gjør en vurdering av opplysninger og ser om avklaringen er nødvendig.
            |
            |Endringer i opplysninger vil automatisk lukke avklaringen om kontrollpunktet sier den ikke lenger er nødvendig.
            |Tilsvarende vil avklaringen åpnes opp igjen om opplysningene endres.
            |
            |## Forklaring av egenskaper
            |
            |**Kan lukkes av saksbehandler** betyr at saksbehandler selv kan markere avklaringen som håndtert uten å endre fakta i behandlingen.
            |Avklaringer som *ikke* kan lukkes av saksbehandler krever at opplysningene i saken faktisk endres – ellers lukkes de ikke.
            |
            |**Lukkes automatisk** betyr at systemet kan lukke avklaringen automatisk når de underliggende opplysningene endres og behovet for avklaringern forsvinner.
            |
            |Avklaringer som *ikke* kan lukkes automatisk må alltid håndteres manuelt av saksbehandler, uavhengig av opplysningene.
            |
            """.trimMargin(),
        )

        val regelsett =
            RegelverkDagpenger.regelsett
                .flatMap { regel -> regel.avklaringer.map { it to regel } }
                .groupBy({ it.first }, { it.second })

        alleAvklaringer.sortedBy { it.kode }.forEach {
            markdown.appendLine("## ${it.tittel}")
            markdown.appendLine("- **Kode:** `${it.kode}`")

            markdown.append("- **Kan lukkes av saksbehandler:** ")
            if (it.kanKvitteres) markdown.appendLine("✅ ") else markdown.appendLine("❌ ")
            markdown.append("- **Lukkes automatisk når opplysningene endres:** ")
            if (it.kanAvbrytes) markdown.appendLine("✅ ") else markdown.appendLine("❌ ")

            if (it.beskrivelse.isNotEmpty()) {
                markdown.appendLine("### Beskrivelse")
                markdown.appendLine("${newlineToBr(it.beskrivelse)}\n")
            }

            if (regelsett[it] != null) {
                markdown.appendLine("### Tilknyttet regelsett")
                regelsett[it]?.forEach { bruktAv ->
                    markdown.appendLine("- [${bruktAv.hjemmel}](./opplysninger.approved.md#${tilMarkdownURL(bruktAv.hjemmel.toString())})")
                }
            }

            val avhengigheterForAvklaring = avhengigheter[it]
            if (!avhengigheterForAvklaring.isNullOrEmpty()) {
                markdown.appendLine("### Opplysninger avklaringen ser på")
                avhengigheterForAvklaring.sortedBy { type -> type.navn }.forEach { type ->
                    markdown.appendLine("- ${type.navn}")
                }
                markdown.appendLine()
            }

            markdown.appendLine("---")
        }

        skriv("avklaringer", markdown.toString())
    }

    private fun finnAvhengigheter(): Map<Avklaringkode, Set<Opplysningstype<*>>> {
        val alleProsessKontrollpunkter =
            listOf(
                Søknadsprosess().kontrollpunkter(),
                Manuellprosess().kontrollpunkter(),
                Omgjøringsprosess().kontrollpunkter(),
            ).flatten()
                .filterIsInstance<Kontrollpunkt>()
                .distinctBy { it.avklaringkode }

        return alleProsessKontrollpunkter.associate { kontrollpunkt ->
            val spy = SpionLesbarOpplysninger()
            try {
                kontrollpunkt.evaluer(spy)
            } catch (_: SpionLesbarOpplysninger.SpionAvsluttet) {
                // Forventet – kontrollpunktet nådde en finnOpplysning()-kall
            } catch (_: Exception) {
                // Annen feil – vi tar det vi har fått så langt
            }
            kontrollpunkt.avklaringkode to spy.brukedeTyper
        }
    }

    private fun tilMarkdownURL(tekst: String) =
        tekst
            .lowercase()
            .replace("§", "")
            .replace(".", "")
            .replace(" ", "-")

    private fun newlineToBr(tekst: String) = tekst.replace("\n", "<br>")

    private companion object {
        val path = "${Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")}/docs/"
        val options = Options().forFile().withExtension(".md")
    }

    private fun skriv(
        filnavn: String,
        innhold: String,
    ) {
        Approvals.namerCreater = Loader { NamerWrapper({ filnavn }, { path }) }
        Approvals.verify(innhold, options)
    }
}
