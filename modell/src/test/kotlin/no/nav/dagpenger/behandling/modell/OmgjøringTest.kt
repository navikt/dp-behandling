package no.nav.dagpenger.behandling.modell

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

class OmgjøringTest {
    @Test
    fun foobar() {
        val søknad = HendelseA(1, "Søknad")
        val behandling1 =
            BehandlingA(søknad).apply {
                opplysninger.add(OpplysningA("sats", "500"))
            }

        val arbeid = HendelseA(2, "Tilbake i arbeid")
        val behandling2 =
            BehandlingA(søknad, basertPå = behandling1).apply {
                opplysninger.add(OpplysningA("sats", "501"))
            }

        val gjenopptak = HendelseA(3, "Gjenopptak")
        val behandling3 =
            BehandlingA(søknad, basertPå = behandling2).apply {
                opplysninger.add(OpplysningA("sats", "502"))
            }

        // Vi har en kjede av behandlinger
        behandling3.basertPå shouldContain behandling2
        behandling2.basertPå shouldContain behandling1
        behandling1.basertPå.shouldBeEmpty()

        // Den siste behandlingen har arvet alle opplysninger
        behandling3.opplysninger shouldHaveSize 3

        // Feil sats mellom hendelse 1 og 2, skulle hatt ekstra barnetillegg
        val feil = HendelseA(4, "Medhold i klage om sats")
        val behandling4 =
            BehandlingA(feil, basertPå = behandling1).apply {
                opplysninger.add(OpplysningA("sats", "532"))
            }

        // Vi har en ny kjede av behandlinger
        behandling3.basertPå shouldContain behandling2
        behandling2.basertPå shouldContain behandling1
        behandling1.basertPå.shouldBeEmpty()
        behandling4.basertPå shouldContain behandling1

        // Den nye behandlingen har arvet opplysninger fra forrige
        behandling4.opplysninger shouldHaveSize 2

        // Nå vil vi merge behandling 3 og 4 - slå sammen HEAD og omgjøring
        val behandling5 = BehandlingA(arbeid, basertPå = mutableSetOf(behandling4, behandling3))

        behandling5.opplysninger shouldHaveSize 4
        behandling5.opplysninger.shouldContainExactly(
            OpplysningA("sats", "500"),
            OpplysningA("sats", "532"),
            OpplysningA("sats", "501"),
            OpplysningA("sats", "502"),
        )

        // En duplikat søknad som vi ikke skjønner hvor skal
        val duplikatSøknad = HendelseA(5, "Duplikat søknad")
        val behandling6 = BehandlingA(duplikatSøknad)

        // Vi skjønner hvor den skal
        behandling6.rebase(behandling5)
        behandling6.opplysninger shouldHaveSize 4
    }
}

data class HendelseA(
    val id: Int,
    val type: String,
)

data class BehandlingA(
    val behandler: HendelseA,
    val basertPå: MutableSet<BehandlingA> = mutableSetOf(),
    val opplysninger: OpplysningerA = mutableSetOf(),
) {
    constructor(behandler: HendelseA, basertPå: BehandlingA) : this(behandler, mutableSetOf(basertPå), mutableSetOf())

    init {
        opplysninger.addAll(basertPå.flatMap { it.opplysninger })
    }

    fun rebase(behandling: BehandlingA) {
        basertPå.add(behandling)
        opplysninger.addAll(behandling.opplysninger)
    }
}

typealias OpplysningerA = MutableSet<OpplysningA>

data class OpplysningA(val navn: String, val verdi: String)
