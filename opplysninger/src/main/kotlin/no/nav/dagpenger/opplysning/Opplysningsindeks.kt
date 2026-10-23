package no.nav.dagpenger.opplysning

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

    /** Gjeldende opplysninger av én type, sortert på fraOgMed. Tom liste om typen ikke finnes. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> gjeldende(type: Opplysningstype<T>): List<Opplysning<T>> {
        oppfrisk(type)
        return gjeldendePerType[type].orEmpty() as List<Opplysning<T>>
    }

    /** Som [gjeldende], men kun egne (ikke-arvede) opplysninger. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> gjeldendeEgne(type: Opplysningstype<T>): List<Opplysning<T>> {
        oppfriskEgen(type)
        return gjeldendeEgnePerType[type].orEmpty() as List<Opplysning<T>>
    }

    /** Alle gjeldende opplysninger på tvers av typer: typer i rekkefølge etter første innsetting, hver type sortert på fraOgMed. */
    val alle: List<Opplysning<*>>
        get() = gjeldendeFlat ?: flatt(gjeldendePerType, ::oppfriskAlle).also { gjeldendeFlat = it }

    /** Som [alle], men kun egne (ikke-arvede) opplysninger. */
    val kunEgne: List<Opplysning<*>>
        get() = gjeldendeEgenFlat ?: flatt(gjeldendeEgnePerType, ::oppfriskAlleEgne).also { gjeldendeEgenFlat = it }

    fun harEgen(opplysning: Opplysning<*>) = egnePerType[opplysning.opplysningstype]?.contains(opplysning) == true

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
        return kilde.values.sortedBy { gruppe -> gruppe.minOf { it.id } }.flatten()
    }

    private fun oppfriskAlle() {
        while (påvirkedeTyper.isNotEmpty()) oppfrisk(påvirkedeTyper.first())
    }

    private fun oppfrisk(type: Opplysningstype<*>) {
        if (!påvirkedeTyper.remove(type)) return
        settGjeldende(gjeldendePerType, type, arvedePerType[type].orEmpty() + egnePerType[type].orEmpty().kunSisteHvisKrevd(type))
    }

    private fun oppfriskAlleEgne() {
        while (påvirkedeEgneTyper.isNotEmpty()) oppfriskEgen(påvirkedeEgneTyper.first())
    }

    private fun oppfriskEgen(type: Opplysningstype<*>) {
        if (!påvirkedeEgneTyper.remove(type)) return
        settGjeldende(gjeldendeEgnePerType, type, egnePerType[type].orEmpty().kunSisteHvisKrevd(type))
    }

    // KUN_SISTE gjelder kun egne opplysninger; arvede (basertPå) tas alltid med
    private fun List<Opplysning<*>>.kunSisteHvisKrevd(type: Opplysningstype<*>) =
        if (type.overlappStrategi == OverlappStrategi.KUN_SISTE && isNotEmpty()) listOf(maxBy { it.id }) else this

    private fun settGjeldende(
        gjeldende: MutableMap<Opplysningstype<*>, List<Opplysning<*>>>,
        type: Opplysningstype<*>,
        rå: List<Opplysning<*>>,
    ) {
        if (rå.isEmpty()) {
            gjeldende.remove(type)
            return
        }
        val utflatet = løsOppOverlappInnadIType(rå, type.overlappStrategi)
        val synlige = utflatet.filterNot { it is Tombstone<*> }
        if (synlige.isEmpty()) gjeldende.remove(type) else gjeldende[type] = synlige
    }

    companion object {
        /**
         * Løser opp overlapp/erstatning/forkorting innad i ÉN opplysningstype. Input må være
         * opplysninger av samme type (typisk en per-type-gruppe). Resultatet er sortert på
         * `gyldighetsperiode.fraOgMed`. Id (innsettingsrekkefølge) avgjør kun hvilke som blir med.
         */
        private fun løsOppOverlappInnadIType(
            gruppe: List<Opplysning<*>>,
            strategi: OverlappStrategi,
        ): List<Opplysning<*>> {
            val dekket = mutableListOf<Gyldighetsperiode>()
            val resultat = mutableListOf<Opplysning<*>>()

            // En revisjon erstatter hele opplysningen den endrer, uavhengig av periode
            val sisteRevisjoner = gruppe.groupBy { it.identitetId }.values.map { rader -> rader.maxBy { it.id } }

            // Nyeste først: en opplysning gjelder kun for de dagene ingen nyere opplysning dekker.
            // Segmentene beholder id-en til opprinnelig opplysning, slik at utledetAv fortsatt peker riktig.
            sisteRevisjoner.sortedByDescending { it.id }.forEach { opplysning ->
                val overlappende = dekket.filter { it.overlapper(opplysning.gyldighetsperiode) }
                val rester = opplysning.gyldighetsperiode.minus(overlappende)
                val synlige =
                    when (strategi) {
                        OverlappStrategi.BEVAR_HØYRE -> rester
                        else -> rester.filter { rest -> overlappende.none { it.erFør(rest) } }
                    }
                synlige.forEach { periode ->
                    resultat.add(if (periode == opplysning.gyldighetsperiode) opplysning else opplysning.medGyldighetsperiode(periode))
                }
                dekket.add(opplysning.gyldighetsperiode)
            }
            return resultat.sortedBy { it.gyldighetsperiode.fraOgMed }
        }
    }
}
