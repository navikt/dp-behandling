package no.nav.dagpenger.regel

import com.spun.util.persistence.Loader
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import java.time.LocalDate
import java.util.UUID

class RegelsettDiagramTest {
    private companion object {
        val path = "${Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")}/docs/"
        val options = Options().forFile().withExtension(".md")
        const val MAKS_KJØRINGER = 2000
        const val MIN_STYRTE_REGELSETT = 5
    }

    private val regelsett = RegelverkDagpenger.regelsett
    private val produsent: Map<Opplysningstype<*>, Regelsett> =
        regelsett.flatMap { rs -> rs.produserer.map { it to rs } }.toMap()

    private val kravPåDagpengerLest by lazy { utforsk(::kravPåDagpenger) }

    private data class Lesing(
        val regelsett: Set<Regelsett>,
        val kravPåDagpenger: Boolean,
    )

    @Test
    fun `printer avhengigheter mellom regelsett`() {
        val noder = regelsett.associateWith { "R${regelsett.indexOf(it)}" }
        val kravNode = "KRAV"
        val avhengigheter = regelsett.associateWith { dataavhengigheter(it) }
        val skalVurderes = regelsett.associateWith { lesing(it, it.skalKjøres) }
        val påvirkerResultat = regelsett.associateWith { lesing(it, it.påvirkerResultat) }

        val kanter = mutableListOf<Pair<String, String>>()

        fun kant(
            fra: String,
            pil: String,
            type: String,
            til: String,
        ) {
            kanter += "  $fra $pil|$type| $til" to type
        }
        regelsett.forEach { rs ->
            avhengigheter.getValue(rs).forEach { kant(noder.getValue(rs), "-->", "data", noder.getValue(it)) }
            skalVurderes.getValue(rs).let { l ->
                l.regelsett.forEach { kant(noder.getValue(rs), "-->", "skalVurderes", noder.getValue(it)) }
                if (l.kravPåDagpenger) kant(noder.getValue(rs), "-->", "skalVurderes", kravNode)
            }
            påvirkerResultat.getValue(rs).let { l ->
                l.regelsett.forEach { kant(noder.getValue(rs), "-->", "påvirkerResultat", noder.getValue(it)) }
                if (l.kravPåDagpenger) kant(noder.getValue(rs), "-->", "påvirkerResultat", kravNode)
            }
        }
        val portvakter =
            regelsett.filter { rs ->
                regelsett.count { andre ->
                    rs in skalVurderes.getValue(andre).regelsett || rs in påvirkerResultat.getValue(andre).regelsett
                } >= MIN_STYRTE_REGELSETT
            }
        val kantfarger = mapOf("data" to "#888888", "skalVurderes" to "#d6336c", "påvirkerResultat" to "#f08c00")

        val mermaid =
            buildList {
                add("graph LR")
                add("  classDef Vilkår fill:#1a5fb4,stroke:#0b3d7a,color:#fff")
                add("  classDef Fastsettelse fill:#2e7d32,stroke:#1b5e20,color:#fff")
                add("  classDef Prosess fill:#b26a00,stroke:#7a4900,color:#fff")
                add("  $kravNode{{\"kravPåDagpenger: alle relevante vilkår\"}}")
                regelsett.forEach { add("  ${noder.getValue(it)}[\"${visningsnavn(it)}\"]:::${it.type}") }
                portvakter.forEach { add("  style ${noder.getValue(it)} stroke:#ffd43b,stroke-width:5px") }
                kanter.forEach { add(it.first) }
                kantfarger.forEach { (type, farge) ->
                    val indekser = kanter.indices.filter { kanter[it].second == type }
                    if (indekser.isNotEmpty()) add("  linkStyle ${indekser.joinToString(",")} stroke:$farge")
                }
            }.joinToString("\n")

        fun navn(
            set: Set<Regelsett>,
            krav: Boolean,
        ) = (set.map { visningsnavn(it) }.sorted() + listOfNotNull("kravPåDagpenger".takeIf { krav }))
            .joinToString(", ")
            .ifEmpty { "-" }

        val tabell =
            regelsett.joinToString("\n") { rs ->
                val data = navn(avhengigheter.getValue(rs), false)
                val vurderes = skalVurderes.getValue(rs).let { navn(it.regelsett, it.kravPåDagpenger) }
                val påvirker = påvirkerResultat.getValue(rs).let { navn(it.regelsett, it.kravPåDagpenger) }
                "| ${visningsnavn(rs)} | $data | $vurderes | $påvirker |"
            }

        skriv(
            listOf(
                "# Regelsett - Dagpenger",
                "",
                "Viser hvordan regelsettene avhenger av hverandre.",
                "",
                "- `data` (grå): regelsettet bruker opplysninger som et annet regelsett produserer",
                "- `skalVurderes` (rosa): regelsettets `skalVurderes` leser opplysninger fra et annet regelsett",
                "- `påvirkerResultat` (oransje): regelsettets `påvirkerResultat` leser opplysninger fra et annet regelsett",
                "- `kravPåDagpenger`: lesingen skjer via `kravPåDagpenger()`, som igjen leser betingelsene til alle relevante vilkår",
                "- Gul tykk kant: portvakt, et regelsett som minst $MIN_STYRTE_REGELSETT andre regelsett styres av via `skalVurderes` eller `påvirkerResultat`",
                "",
                "Leste opplysninger i `skalVurderes`/`påvirkerResultat` er funnet ved å kjøre lambdaene mot et",
                "opptakende opplysningssett som utforsker alle utfall av hver lesing.",
                "",
                "## Graf",
                "",
                "```mermaid",
                mermaid,
                "```",
                "",
                "## Tabell",
                "",
                "| Regelsett | Data fra | skalVurderes avhenger av | påvirkerResultat avhenger av |",
                "| --- | --- | --- | --- |",
                tabell,
                "",
            ).joinToString("\n"),
        )
    }

    private fun visningsnavn(rs: Regelsett) = "${rs.navn} (${rs.type})"

    private fun dataavhengigheter(rs: Regelsett) =
        rs.avhengerAv
            .mapNotNull { produsent[it] }
            .filter { it != rs }
            .toSet()

    private fun lesing(
        rs: Regelsett,
        lambda: (LesbarOpplysninger) -> Boolean,
    ): Lesing {
        val leste = utforsk(lambda)
        val viaKrav = leste.containsAll(kravPåDagpengerLest)
        val egne = if (viaKrav) leste - kravPåDagpengerLest else leste
        return Lesing(egne.mapNotNull { produsent[it] }.filter { it != rs }.toSet(), viaKrav)
    }

    private fun utforsk(lambda: (LesbarOpplysninger) -> Boolean): Set<Opplysningstype<*>> {
        val leste = mutableSetOf<Opplysningstype<*>>()
        val kø = ArrayDeque<List<Boolean>>().apply { add(emptyList()) }
        var kjøringer = 0
        while (kø.isNotEmpty() && kjøringer++ < MAKS_KJØRINGER) {
            val prefiks = kø.removeFirst()
            val opptaker = Opptaker(prefiks)
            runCatching { lambda(opptaker) }
            leste += opptaker.leste
            opptaker.valg.forEachIndexed { i, _ ->
                if (i >= prefiks.size) kø.add(opptaker.valg.take(i) + true)
            }
        }
        return leste
    }

    private class Opptaker(
        private val manus: List<Boolean>,
    ) : LesbarOpplysninger {
        val leste = linkedSetOf<Opplysningstype<*>>()
        val valg = mutableListOf<Boolean>()
        private val svar = mutableMapOf<Opplysningstype<*>, Boolean>()

        private fun svarFor(type: Opplysningstype<*>): Boolean {
            leste += type
            return svar.getOrPut(type) {
                val verdi = manus.getOrElse(valg.size) { false }
                valg += verdi
                verdi
            }
        }

        override val id: UUID = UUID.randomUUID()
        override val kunEgne get() = this

        override fun <T : Any> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> {
            leste += opplysningstype
            error("Ikke tilgjengelig")
        }

        override fun <T : Any> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>? {
            leste += opplysningstype
            return null
        }

        override fun <T : Any> har(opplysningstype: Opplysningstype<T>) = svarFor(opplysningstype)

        override fun erSann(opplysningstype: Opplysningstype<Boolean>) = svarFor(opplysningstype)

        override fun finnFlere(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>> {
            leste += opplysningstyper
            return emptyList()
        }

        override fun <T : Any> finnAlle(opplysningstyper: List<Opplysningstype<T>>): List<Opplysning<T>> {
            leste += opplysningstyper
            return emptyList()
        }

        override fun <T : Any> finnAlle(opplysningstype: Opplysningstype<T>): List<Opplysning<T>> {
            leste += opplysningstype
            return emptyList()
        }

        override fun finnOpplysning(opplysningId: UUID): Opplysning<*> = error("Ikke tilgjengelig")

        override fun forDato(gjelderFor: LocalDate) = this

        override fun erErstattet(opplysninger: List<Opplysning<*>>) = false

        override fun erEgen(opplysning: Opplysning<*>) = false

        override fun somListe(filter: LesbarOpplysninger.Filter): List<Opplysning<*>> = emptyList()
    }

    private fun skriv(dokumentasjon: String) {
        Approvals.namerCreater = Loader { NamerWrapper({ "regelsett-dagpenger" }, { path }) }
        Approvals.verify(dokumentasjon, options)
    }
}
