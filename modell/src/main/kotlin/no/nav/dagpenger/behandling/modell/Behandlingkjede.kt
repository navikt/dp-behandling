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

infix fun Behandlingkjede.med(barn: Behandling) =
    copy(
        barn = this.barn.plusElement(barn.somKjede()),
    )

operator fun Behandlingkjede.contains(behandling: Behandling): Boolean = rot === behandling || barn.any { behandling in it }
