package no.nav.dagpenger.brev

import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.PengeVerdiDTO
import no.nav.dagpenger.behandling.api.models.TekstVerdiDTO
import java.util.UUID

class BrevBygger(
    private val maltekster: List<Maltekst>,
) {
    constructor(brevmal: Brevmal) : this(brevmal.maltekster)

    fun bygg(resultat: BehandlingsresultatDTO): Brev {
        val kontekst = BrevKontekst(resultat)
        val aktiveMaltekster =
            maltekster
                .filter { kontekst.evaluerer(it.trigger) }
                .sortedWith(compareBy({ it.plassering.ordinal }, { it.rekkefølge }))

        val overskrift =
            aktiveMaltekster
                .filter { it.plassering == Plassering.OVERSKRIFT }
                .joinToString(" ") { kontekst.interpoler(it.tekst) }

        val seksjoner =
            aktiveMaltekster
                .filter { it.plassering != Plassering.OVERSKRIFT }
                .groupBy { it.plassering }
                .map { (plassering, tekster) ->
                    Brevseksjon(
                        plassering = plassering,
                        innhold = tekster.map { kontekst.interpoler(it.tekst) },
                    )
                }

        return Brev(
            overskrift = overskrift,
            seksjoner = seksjoner,
        )
    }
}

internal class BrevKontekst(
    private val resultat: BehandlingsresultatDTO,
) {
    private val opplysningerMap: Map<UUID, OpplysningerDTO> =
        resultat.opplysninger.associateBy { it.opplysningTypeId }

    private val opplysningerNavnMap: Map<String, OpplysningerDTO> =
        resultat.opplysninger.associateBy { it.navn }

    fun evaluerer(trigger: Trigger): Boolean =
        when (trigger) {
            is Trigger.Alltid -> true
            is Trigger.Avgjørelse -> resultat.førteTil.value.equals(trigger.avgjørelse, ignoreCase = true)
            is Trigger.OpplysningFinnes -> trigger.opplysningsTypeId in opplysningerMap
            is Trigger.OpplysningVerdi -> {
                val opplysning = opplysningerMap[trigger.opplysningsTypeId]
                opplysning != null && sisteVerdi(opplysning) == trigger.forventetVerdi
            }
        }

    fun interpoler(tekst: String): String =
        PLACEHOLDER_REGEX.replace(tekst) { match ->
            val nøkkel = match.groupValues[1]
            oppslåVerdi(nøkkel) ?: match.value
        }

    private fun oppslåVerdi(nøkkel: String): String? =
        when (nøkkel.lowercase()) {
            "avgjørelse" -> resultat.førteTil.value
            "ident" -> resultat.ident
            else -> {
                val opplysning = opplysningerNavnMap[nøkkel]
                opplysning?.let { sisteVerdi(it) }
            }
        }

    private fun sisteVerdi(opplysning: OpplysningerDTO): String? {
        val sistePeriode = opplysning.perioder.lastOrNull() ?: return null
        return when (val verdi = sistePeriode.verdi) {
            is BoolskVerdiDTO -> verdi.verdi.toString()
            is DatoVerdiDTO -> verdi.verdi.toString()
            is DesimaltallVerdiDTO -> verdi.verdi.toString()
            is HeltallVerdiDTO -> verdi.verdi.toString()
            is PengeVerdiDTO -> verdi.verdi.toString()
            is TekstVerdiDTO -> verdi.verdi
            else -> verdi.toString()
        }
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{\\{(.+?)}}")
    }
}
