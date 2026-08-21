package no.nav.dagpenger.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Opplysninger.Companion.forkortetTil

/**
 * Holder de gjeldende (overlapp-oppløste) opplysningene per opplysningstype for én
 * [Opplysninger]-instans, og husker hvilke typer som er påvirket av siste mutasjon - slik at
 * bare disse må løses opp på nytt, i stedet for alle typer i behandlingen.
 *
 * Rå kilder er delt i to: [arvedePerType] (fra en tidligere behandling i en revurderingskjede,
 * immutabel etter konstruksjon) og opplysninger lagt til i denne behandlingen ("egne"), som
 * vedlikeholdes inkrementelt via [leggTilEgen]/[fjernEgen].
 *
 * Merk: cachen forutsetter at `id` og `gyldighetsperiode` på en [Opplysning] er immutable -
 * om det noen gang endres, må cachen invalideres eksplisitt.
 */
internal class Opplysningsindeks(
    private val arvedePerType: Map<Opplysningstype<*>, List<Opplysning<*>>>,
    egneVedOppstart: List<Opplysning<*>>,
) {
    private val egnePerType: MutableMap<Opplysningstype<*>, MutableList<Opplysning<*>>> =
        egneVedOppstart.groupByTo(mutableMapOf()) { it.opplysningstype }

    private val gjeldendePerType = mutableMapOf<Opplysningstype<*>, List<Opplysning<*>>>()
    private val gjeldendeEgnePerType = mutableMapOf<Opplysningstype<*>, List<Opplysning<*>>>()

    // To uavhengige dirty-sets: "alle" (arvet+egen) leses på nesten hvert oppslag under
    // regelkjøring, mens "kunEgne" bare trengs av et fåtall kallere (API-mapping, forDato-visning
    // i regelkjøring). Ved å holde dem separate slår vi ikke opp/oppløser kunEgne-varianten for
    // typer ingen faktisk spør om kunEgne for.
    private val påvirkedeTyper: MutableSet<Opplysningstype<*>> = (arvedePerType.keys + egnePerType.keys).toMutableSet()
    private val påvirkedeEgneTyper: MutableSet<Opplysningstype<*>> = egnePerType.keys.toMutableSet()
    private var gjeldendeFlat: List<Opplysning<*>>? = null
    private var gjeldendeEgenFlat: List<Opplysning<*>>? = null

    /** Gjeldende opplysninger av én type, id-sortert (= "nyeste sist"). Tom liste om typen ikke finnes. */
    fun gjeldende(type: Opplysningstype<*>): List<Opplysning<*>> {
        oppfrisk(type)
        return gjeldendePerType[type].orEmpty()
    }

    /** Alle gjeldende opplysninger på tvers av typer, globalt id-sortert. */
    val alle: List<Opplysning<*>>
        get() = gjeldendeFlat ?: flatt(gjeldendePerType, ::oppfriskAlle).also { gjeldendeFlat = it }

    /** Kun egne (ikke-arvede) gjeldende opplysninger, globalt id-sortert. */
    val kunEgne: List<Opplysning<*>>
        get() = gjeldendeEgenFlat ?: flatt(gjeldendeEgnePerType, ::oppfriskAlleEgne).also { gjeldendeEgenFlat = it }

    fun leggTilEgen(opplysning: Opplysning<*>) {
        egnePerType.getOrPut(opplysning.opplysningstype) { mutableListOf() }.add(opplysning)
        merkPåvirket(opplysning.opplysningstype)
    }

    fun fjernEgen(opplysning: Opplysning<*>) {
        egnePerType[opplysning.opplysningstype]?.remove(opplysning)
        merkPåvirket(opplysning.opplysningstype)
    }

    private fun merkPåvirket(type: Opplysningstype<*>) {
        påvirkedeTyper += type
        påvirkedeEgneTyper += type
        gjeldendeFlat = null
        gjeldendeEgenFlat = null
    }

    private fun flatt(
        kilde: Map<Opplysningstype<*>, List<Opplysning<*>>>,
        oppfriskAlt: () -> Unit,
    ): List<Opplysning<*>> {
        oppfriskAlt()
        return kilde.values.flatten().sortedBy { it.id }
    }

    private fun oppfriskAlle() {
        while (påvirkedeTyper.isNotEmpty()) oppfrisk(påvirkedeTyper.first())
    }

    private fun oppfrisk(type: Opplysningstype<*>) {
        if (!påvirkedeTyper.remove(type)) return
        settGjeldende(gjeldendePerType, type, arvedePerType[type].orEmpty() + egnePerType[type].orEmpty())
    }

    private fun oppfriskAlleEgne() {
        while (påvirkedeEgneTyper.isNotEmpty()) oppfriskEgen(påvirkedeEgneTyper.first())
    }

    private fun oppfriskEgen(type: Opplysningstype<*>) {
        if (!påvirkedeEgneTyper.remove(type)) return
        settGjeldende(gjeldendeEgnePerType, type, egnePerType[type].orEmpty())
    }

    private fun settGjeldende(
        gjeldende: MutableMap<Opplysningstype<*>, List<Opplysning<*>>>,
        type: Opplysningstype<*>,
        rå: List<Opplysning<*>>,
    ) {
        if (rå.isEmpty()) {
            gjeldende.remove(type)
            return
        }
        // Invariant: oppslag som bruker lastOrNull() (f.eks. finnNullableOpplysning/har) forventer
        // "nyeste" - dvs. id-sortert. Resolve-algoritmen sorterer derimot på fraOgMed, så vi må
        // eksplisitt re-sortere på id før gruppen lagres i cachen.
        gjeldende[type] = løsOppOverlappInnadIType(rå).sortedBy { it.id }
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Løser opp overlapp/erstatning/forkorting innad i ÉN opplysningstype. Input må være
         * opplysninger av samme type (typisk en per-type-gruppe). Resultatet er sortert på
         * `gyldighetsperiode.fraOgMed` - kalleren (se [settGjeldende]) sørger selv for å
         * re-sortere på `id` etterpå.
         */
        private fun løsOppOverlappInnadIType(gruppe: List<Opplysning<*>>): List<Opplysning<*>> {
            val sortert = gruppe.sortedBy { it.gyldighetsperiode.fraOgMed }
            val resultat = mutableListOf<Opplysning<*>>()

            sortert.forEach { utfordrer ->
                val forrige = resultat.lastOrNull()

                when {
                    forrige == null -> {
                        resultat.add(utfordrer)
                    }

                    forrige.gyldighetsperiode.erFør(utfordrer.gyldighetsperiode) ||
                        forrige.gyldighetsperiode.tilstøter(utfordrer.gyldighetsperiode) -> {
                        resultat.add(utfordrer)
                    }

                    // det er overlapp, men forrige er nyest
                    forrige.id > utfordrer.id -> {}

                    // utfordrer er nyest, og overskriver hele forrige
                    forrige.gyldighetsperiode.fraOgMed == utfordrer.gyldighetsperiode.fraOgMed -> {
                        resultat[resultat.lastIndex] = utfordrer
                    }

                    else -> {
                        val forkortet = forrige.forkortetTil(utfordrer)
                        logger.debug {
                            """
                                |Kant-i-kant overlapper opplysning ${forrige.id} og ${utfordrer.id} for type ${forrige.opplysningstype.navn}. Lager forkortet opplysning.
                                |Venstre: ${forrige.gyldighetsperiode}
                                |Høyre: ${utfordrer.gyldighetsperiode}
                                |Forkortet: ${forkortet.gyldighetsperiode}
                            """.trimMargin()
                        }
                        resultat[resultat.lastIndex] = forkortet
                        resultat.add(utfordrer)
                    }
                }
            }
            return resultat
        }
    }
}
