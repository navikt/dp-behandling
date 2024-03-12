package no.nav.dagpenger.behandling.modell

import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.behandling.modell.hendelser.OpplysningSvarHendelse
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse

class Person(
    private val ident: PersonIdentifikator,
    private val behandlinger: MutableList<Behandling> = mutableListOf(),
) : Aktivitetskontekst {
    fun ident() = ident

    fun håndter(hendelse: SøknadInnsendtHendelse) {
        hendelse.leggTilKontekst(this)
        hendelse.info("Mottatt søknad og startet behandling")
        hendelse.varsel(Behandlingsvarsler.SØKNAD_MOTTATT)
        val behandling =
            Behandling(
                hendelse,
                emptyList(),
            ).also { behandling ->
                behandlinger.add(behandling)
            }
        behandling.behandle(hendelse)
    }

    fun håndter(hendelse: OpplysningSvarHendelse) {
        hendelse.leggTilKontekst(this)
        val behandling = behandlinger.first() // .first { it.behandlingId == hendelse.behandlingId }
        behandling.behandle(hendelse)
    }

    private fun PersonHendelse.leggTilKontekst(kontekst: Aktivitetskontekst) {
        kontekst(this)
        kontekst(kontekst)
    }

    // todo: vi trenger en metode for å hente behandlinger fra utsiden.
    fun behandlinger() = behandlinger.toList()

    override fun toSpesifikkKontekst(): SpesifikkKontekst = PersonKontekst(ident.identifikator())

    data class PersonKontekst(val ident: String) : SpesifikkKontekst("Person") {
        override val kontekstMap = mapOf("ident" to ident)
    }
}
