package no.nav.dagpenger.behandling.modell

data class Behandlingkjede(
    val rot: Behandling,
    val barn: List<Behandlingkjede> = emptyList(),
) {
    val erLøvnode = barn.isEmpty()
    val dybde: Int = if (erLøvnode) 0 else barn.maxOf { it.dybde } + 1
    val etterkommere: Int = barn.sumOf { it.etterkommere } + barn.count()

    init {
        check(barn.all { it.rot.basertPå === rot }) {
            "Forventer at alle barn peker på samme objektreferanse som forelder"
        }
    }
}

fun Behandling.somKjede() = Behandlingkjede(this)

infix fun Behandlingkjede.leggTil(barn: Behandling): Behandlingkjede {
    checkNotNull(barn.basertPå) { "kan ikke legge til barn utenom en forelder" }
    check(barn.basertPå in this) { "kan ikke legge til barn med en forelder som ikke finnes i kjeden" }
    return this.leggTilBarnHvisDelAvGren(barn)
}

private fun Behandlingkjede.leggTilBarnHvisDelAvGren(barn: Behandling): Behandlingkjede =
    if (barn.basertPå!! === this.rot) {
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

enum class Gåmetode { BreddeFørst, DybdeFørst }

fun Behandlingkjede.gåGjennom(
    gåmetode: Gåmetode,
    block: (Behandling) -> Unit,
) {
    val stabel = ArrayDeque<Behandlingkjede>(listOf(this))
    while (stabel.isNotEmpty()) {
        val gjeldende =
            when (gåmetode) {
                Gåmetode.BreddeFørst -> stabel.removeFirst()
                Gåmetode.DybdeFørst -> stabel.removeLast()
            }

        block(gjeldende.rot)

        stabel.addAll(
            when (gåmetode) {
                Gåmetode.BreddeFørst -> gjeldende.barn
                Gåmetode.DybdeFørst -> gjeldende.barn.reversed()
            },
        )
    }
}
