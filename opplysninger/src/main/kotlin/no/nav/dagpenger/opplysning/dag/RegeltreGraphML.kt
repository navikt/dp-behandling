package no.nav.dagpenger.opplysning.dag

import no.nav.dagpenger.dag.DAG
import no.nav.dagpenger.dag.Edge
import no.nav.dagpenger.dag.Node
import no.nav.dagpenger.dag.printer.GraphMLPrinter
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.KvoteDefinisjon
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.Regel

/**
 * Regeltreet som GraphML med regelsett, avklaringer og kvoter som egne noder.
 *
 * Opplysning -[regeltype]-> Opplysning er regeltreet (fra produkt til det det utledes av).
 * Regelsett kobles til opplysninger med PRODUSERER, AVHENGER_AV, HENTER_VIA_BEHOV, UTFALL, BETINGELSE,
 * ØNSKET_RESULTAT og AKTIV_OPPLYSNING, og til avklaringer og kvoter med HAR_AVKLARING og HAR_KVOTE.
 * Typer (Vilkår, Fastsettelse, Prosess og datatype) er egenskaper, ikke labels.
 */
class RegeltreGraphML(
    private val regelsett: List<Regelsett>,
) {
    private val produsentRegelsett: Map<Opplysningstype<*>, Regelsett> =
        regelsett.flatMap { rs -> rs.produserer.map { it to rs } }.toMap()

    private val produsentRegel: Map<Opplysningstype<*>, Regel<*>> =
        regelsett.flatMap { it.regler() }.associateBy { it.produserer }

    fun toPrint(): String = GraphMLPrinter(dag(), ::nodeEgenskaper, ::kantEgenskaper).toPrint()

    private fun dag(): DAG<Any, Any> {
        val regeltre = RegeltreBygger(regelsett.flatMap { it.regler() }).dag()
        val kanter =
            regeltre.edges.map {
                Edge<Any, Any>(
                    Node(it.from.name, it.from.data),
                    Node(it.to.name, it.to.data),
                    it.edgeName,
                    it.data,
                )
            }
        return DAG(kanter + regelsett.flatMap(::regelsettKanter))
    }

    private fun regelsettKanter(rs: Regelsett): List<Edge<Any, Any>> {
        val fra = regelsettNode(rs)

        fun til(
            relasjon: String,
            opplysninger: Collection<Opplysningstype<*>>,
        ) = opplysninger.map { Edge<Any, Any>(fra, opplysningNode(it), relasjon) }

        val betingelser = rs.betingelser.toSet()
        val ønsketResultat = rs.ønsketInformasjon - betingelser - setOfNotNull(rs.utfall)

        return til("PRODUSERER", rs.produserer) +
            til("AVHENGER_AV", rs.avhengerAv) +
            til("HENTER_VIA_BEHOV", rs.behov) +
            til("UTFALL", listOfNotNull(rs.utfall)) +
            til("BETINGELSE", betingelser) +
            til("ØNSKET_RESULTAT", ønsketResultat) +
            til("AKTIV_OPPLYSNING", listOfNotNull(rs.aktivOpplysning)) +
            rs.avklaringer.map { Edge<Any, Any>(fra, avklaringNode(it), "HAR_AVKLARING") } +
            rs.kvoter.flatMap { kvote ->
                val kvoteNode = kvoteNode(kvote)
                listOf(Edge<Any, Any>(fra, kvoteNode, "HAR_KVOTE")) + kvoteKanter(kvoteNode, kvote)
            }
    }

    private fun kvoteKanter(
        fra: Node<Any>,
        kvote: KvoteDefinisjon,
    ) = listOf(
        "TILDELINGSGRUNNLAG" to kvote.tildelingsgrunnlag.kapasitet,
        "TELLES_NÅR" to kvote.tellesNår,
        "UTLØSENDE_BETINGELSE" to kvote.utløsendeBetingelse,
        "FORBRUKSTELLER" to kvote.forbruksteller,
        "GJENSTÅENDE" to kvote.gjenstående,
        "SISTE_FORBRUK" to kvote.sisteForbruk,
        "SISTE_GJENSTÅENDE" to kvote.sisteGjenstående,
    ).map { (relasjon, type) -> Edge<Any, Any>(fra, opplysningNode(type), relasjon) }

    private fun opplysningNode(type: Opplysningstype<*>) = Node<Any>(type.navn, type)

    private fun regelsettNode(rs: Regelsett) = Node<Any>(rs.navn, rs)

    private fun avklaringNode(kode: Avklaringkode) = Node<Any>(kode.kode, kode)

    private fun kvoteNode(kvote: KvoteDefinisjon) = Node<Any>(kvote.navn, kvote)

    private fun nodeEgenskaper(node: Node<Any>): Map<String, String?> =
        when (val data = node.data) {
            is Opplysningstype<*> -> opplysningEgenskaper(data)
            is Regelsett -> regelsettEgenskaper(data)
            is Avklaringkode -> avklaringEgenskaper(data)
            is KvoteDefinisjon -> kvoteEgenskaper(data)
            else -> emptyMap()
        }

    private fun opplysningEgenskaper(type: Opplysningstype<*>): Map<String, String?> {
        val rs = produsentRegelsett[type]
        val regel = produsentRegel[type]
        val grunnlag = regel == null || regel.avhengerAv.isEmpty()
        return mapOf(
            "labels" to ":Opplysning" + if (grunnlag) ":Grunnlag" else "",
            "opplysningId" to type.id.uuid.toString(),
            "regelsett" to rs?.navn,
            "regelsettType" to rs?.type?.name,
            "regelType" to regel?.javaClass?.simpleName,
            "datatype" to type.datatype.navn(),
            "enhet" to type.enhet?.name,
            "gyldighetsperiodeStrategi" to stabiltNavn(type.gyldighetsperiodeStrategi.navn),
            "overlappStrategi" to type.overlappStrategi.name,
            "kategori" to type.opplysningstypeKategori.name,
            "formål" to type.formål.name,
            "behovId" to type.behovId,
        )
    }

    private fun regelsettEgenskaper(rs: Regelsett) =
        mapOf(
            "labels" to ":Regelsett",
            "navn" to rs.navn,
            "type" to rs.type.name,
            "hjemmel" to rs.hjemmel.toString(),
            "hjemmelUrl" to rs.hjemmel.url?.toString(),
        )

    private fun avklaringEgenskaper(avklaring: Avklaringkode) =
        mapOf(
            "labels" to ":Avklaring",
            "kode" to avklaring.kode,
            "tittel" to avklaring.tittel,
            "beskrivelse" to avklaring.beskrivelse,
            "kanKvitteres" to avklaring.kanKvitteres.toString(),
            "kanAvbrytes" to avklaring.kanAvbrytes.toString(),
        )

    private fun kvoteEgenskaper(kvote: KvoteDefinisjon) =
        mapOf(
            "labels" to ":Kvote",
            "navn" to kvote.navn,
            "forbrukstype" to kvote.forbrukstype.name,
            "hjemmel" to kvote.hjemmel.toString(),
        )

    private fun kantEgenskaper(kant: Edge<Any, Any>): Map<String, String?> =
        mapOf(
            "label" to kant.edgeName,
            "regelsett" to (kant.data as? Regel<*>)?.let { produsentRegelsett[it.produserer]?.navn },
        )

    // Anonyme lambdaer har klassenavn med adresse som endrer seg mellom kjøringer
    private fun stabiltNavn(navn: String) = if ("\$\$Lambda" in navn) "egendefinert:${navn.substringBefore("\$\$Lambda")}" else navn
}
