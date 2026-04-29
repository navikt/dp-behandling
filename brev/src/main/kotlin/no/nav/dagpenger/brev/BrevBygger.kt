package no.nav.dagpenger.brev

import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.api.models.BoolskVerdiDTO
import no.nav.dagpenger.behandling.api.models.DatoVerdiDTO
import no.nav.dagpenger.behandling.api.models.DesimaltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.HeltallVerdiDTO
import no.nav.dagpenger.behandling.api.models.OpplysningerDTO
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
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

        // Grupper maltekster til seksjoner. Maltekster med tittel starter en ny seksjon.
        val seksjoner = mutableListOf<Brevseksjon>()
        var gjeldendePlassering: Plassering? = null
        var gjeldendeTittel: String? = null
        var gjeldendeTekster = mutableListOf<String>()

        for (maltekst in aktiveMaltekster.filter { it.plassering != Plassering.OVERSKRIFT }) {
            val harNyTittel = maltekst.tittel != null
            val nyPlassering = maltekst.plassering != gjeldendePlassering

            if ((harNyTittel || nyPlassering) && gjeldendeTekster.isNotEmpty()) {
                seksjoner.add(
                    Brevseksjon(
                        plassering = gjeldendePlassering!!,
                        tittel = gjeldendeTittel,
                        innhold = gjeldendeTekster.toList(),
                    ),
                )
                gjeldendeTekster = mutableListOf()
            }

            if (harNyTittel || nyPlassering) {
                gjeldendeTittel = maltekst.tittel?.let { kontekst.interpoler(it) }
            }
            gjeldendePlassering = maltekst.plassering
            gjeldendeTekster.add(kontekst.interpoler(maltekst.tekst))
        }

        if (gjeldendeTekster.isNotEmpty() && gjeldendePlassering != null) {
            seksjoner.add(
                Brevseksjon(
                    plassering = gjeldendePlassering,
                    tittel = gjeldendeTittel,
                    innhold = gjeldendeTekster.toList(),
                ),
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
            is Trigger.OpplysningFinnes -> {
                val opplysning = opplysningerMap[trigger.opplysningsTypeId]
                opplysning != null &&
                    (!trigger.kunNyeOpplysninger || harNyePerioder(opplysning)) &&
                    (trigger.periodeType == null || matcherPeriodeType(opplysning, trigger.periodeType))
            }
            is Trigger.OpplysningVerdi -> {
                val opplysning = opplysningerMap[trigger.opplysningsTypeId]
                opplysning != null &&
                    sisteVerdi(opplysning) == trigger.forventetVerdi &&
                    (!trigger.kunNyeOpplysninger || harNyePerioder(opplysning))
            }
        }

    fun interpoler(tekst: String): String =
        PLACEHOLDER_REGEX.replace(tekst) { match ->
            val nøkkel = match.groupValues[1]
            oppslåVerdi(nøkkel) ?: match.value
        }

    private fun matcherPeriodeType(
        opplysning: OpplysningerDTO,
        periodeType: PeriodeType,
    ): Boolean {
        val perioder = opplysning.perioder
        return when (periodeType) {
            PeriodeType.ÅPEN -> perioder.size == 1 && perioder[0].gyldigTilOgMed == null
            PeriodeType.LUKKET -> perioder.size == 1 && perioder[0].gyldigTilOgMed != null
            PeriodeType.FLERE -> perioder.size > 1
        }
    }

    private fun harNyePerioder(opplysning: OpplysningerDTO): Boolean = opplysning.perioder.any { it.opprinnelse == OpprinnelseDTO.NY }

    private fun oppslåVerdi(nøkkel: String): String? {
        // Støtter "opplysningsnavn.fraOgMed" og "opplysningsnavn.tilOgMed"
        if ("." in nøkkel) {
            val (navn, felt) = nøkkel.split(".", limit = 2)
            val opplysning = opplysningerNavnMap[navn] ?: return null
            val sistePeriode = opplysning.perioder.lastOrNull() ?: return null
            return when (felt.lowercase()) {
                "fraogmed", "fra", "gyldigfraogmed" -> sistePeriode.gyldigFraOgMed?.toString()
                "tilogmed", "til", "gyldigtilogmed" -> sistePeriode.gyldigTilOgMed?.toString()
                else -> null
            }
        }
        return when (nøkkel.lowercase()) {
            "avgjørelse" -> resultat.førteTil.value
            "ident" -> resultat.ident
            else -> {
                val opplysning = opplysningerNavnMap[nøkkel]
                opplysning?.let { sisteVerdi(it) }
            }
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
