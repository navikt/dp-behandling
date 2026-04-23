package no.nav.dagpenger.behandling.modell

data class Behandlingkjede(
    val rot: Behandling,
    val barn: List<Behandlingkjede> = emptyList(),
) : Iterable<Behandling> {
    val erLøvnode = barn.isEmpty()
    val dybde: Int = if (erLøvnode) 0 else barn.maxOf { it.dybde } + 1
    val etterkommere: Int = barn.sumOf { it.etterkommere } + barn.count()
    val erFerdig = rot.harTilstand(Behandling.TilstandType.Ferdig)

    init {
        check(barn.all { it.rot.basertPå === rot }) {
            "Forventer at alle barn peker på samme objektreferanse som forelder"
        }
    }

    // traverserer treet bredde først
    override fun iterator(): Iterator<Behandling> {
        return object : Iterator<Behandling> {
            private val stabel = ArrayDeque(listOf(this@Behandlingkjede))

            override fun hasNext() = stabel.isNotEmpty()

            override fun next(): Behandling {
                val gjeldende = stabel.removeFirst()
                stabel.addAll(gjeldende.barn)
                return gjeldende.rot
            }
        }
    }
}

fun Behandling.somKjede() = Behandlingkjede(this)

fun List<Behandling>.somKjede(): Behandlingkjede {
    check(isNotEmpty())
    val rot = first()
    check(rot.basertPå == null) { "forventer at første element i listen er roten" }
    return drop(1).fold(rot.somKjede()) { kjede, behandling ->
        kjede leggTil behandling
    }
}

infix fun Behandlingkjede.leggTil(barn: Behandling): Behandlingkjede {
    checkNotNull(barn.basertPå) { "kan ikke legge til barn utenom en forelder" }
    check(barn.basertPå in this) { "kan ikke legge til barn med en forelder som ikke finnes i kjeden" }
    return this.leggTilBarnHvisDelAvGren(barn)
}

private fun Behandlingkjede.leggTilBarnHvisDelAvGren(barn: Behandling): Behandlingkjede =
    if (barn.basertPå!! === this.rot) {
        check(this.erFerdig) { "kan ikke legge til ny behandling på forelder som er uferdig" }
        copy(
            barn = this.barn.plusElement(barn.somKjede()),
        )
    } else {
        copy(
            barn =
                this.barn.map { gren ->
                    if (barn.basertPå in this) {
                        gren.leggTilBarnHvisDelAvGren(barn)
                    } else {
                        gren
                    }
                },
        )
    }

operator fun Behandlingkjede.contains(behandling: Behandling): Boolean = rot === behandling || barn.any { behandling in it }

// går gjennom hele treet for å finne løvnoder å bygge videre på,
// for å avdekke evt. korrupte trær
fun Behandlingkjede.denBehandlingenViSkalBasereNyPå(): Behandling? {
    val stabel = ArrayDeque<Behandlingkjede>(listOf(this))
    val kandidater = mutableListOf<Behandling>()
    while (stabel.isNotEmpty()) {
        val gjeldende = stabel.removeFirst()

        if (gjeldende.erFerdig && gjeldende.barn.none { it.erFerdig }) {
            kandidater.add(gjeldende.rot)
        }

        stabel.addAll(gjeldende.barn)
    }

    check(kandidater.size <= 1) { "korrupt tre! det er mer enn en behandling som har tilstand ferdig og som er løvnode" }
    return kandidater.firstOrNull()
}
