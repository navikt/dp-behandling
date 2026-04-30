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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class BrevBygger(
    private val brevmal: Brevmal,
) {
    constructor(maltekster: List<Maltekst>) : this(Brevmal(navn = "", maltekster = maltekster))

    /**
     * Bygger et brev fra behandlingsresultatet.
     * Returnerer null hvis malen har [Brevmal.krevInnholdI] satt og ingen av de
     * krevde plasseringene fikk innhold (f.eks. kun meldekort-endringer).
     */
    fun bygg(resultat: BehandlingsresultatDTO): Brev? {
        val kontekst = BrevKontekst(resultat)
        val aktiveMaltekster =
            brevmal.maltekster
                .filter { kontekst.evaluerer(it.trigger) }
                .sortedWith(compareBy({ it.plassering.ordinal }, { it.rekkefølge }))

        // Sjekk om krevde plasseringer har innhold
        if (brevmal.krevInnholdI.isNotEmpty()) {
            val harKrevdInnhold = aktiveMaltekster.any { it.plassering in brevmal.krevInnholdI }
            if (!harKrevdInnhold) return null
        }

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
            is Trigger.Avgjørelse ->
                resultat.førteTil.value.let { v ->
                    trigger.avgjørelser.any { it.equals(v, ignoreCase = true) }
                }
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
                    (!trigger.kunNyeOpplysninger || harNyePerioder(opplysning)) &&
                    (trigger.periodeType == null || matcherPeriodeType(opplysning, trigger.periodeType))
            }
        }

    fun interpoler(tekst: String): String =
        PLACEHOLDER_REGEX.replace(tekst) { match ->
            val uttrykk = match.groupValues[1].trim()
            evaluerUttrykk(uttrykk) ?: match.value
        }

    /**
     * Evaluerer et template-uttrykk med valgfri pipe-syntaks.
     * Eksempler:
     *   "Siste avsluttende kalendermåned" → slår opp verdien direkte
     *   "Siste avsluttende kalendermåned | månedÅr(0)" → formaterer som "mars 2026"
     *   "Siste avsluttende kalendermåned | månedÅr(-12)" → trekker fra 12 mnd, formaterer som "april 2025"
     */
    private fun evaluerUttrykk(uttrykk: String): String? {
        if ("|" !in uttrykk) return oppslåVerdi(uttrykk)

        val deler = uttrykk.split("|").map { it.trim() }
        val nøkkel = deler[0]
        val makroer = deler.drop(1)

        // Kjør makroene sekvensielt — output fra en blir input til neste
        var resultat: String? = null
        for ((index, makro) in makroer.withIndex()) {
            if (index == 0) {
                resultat = utførMakro(nøkkel, makro)
            } else {
                // For kjeding: bruk forrige resultat som en streng-transformasjon
                resultat = resultat?.let { utførStringMakro(it, makro) }
            }
            if (resultat == null) return null
        }
        return resultat
    }

    private fun oppslåDato(nøkkel: String): LocalDate? {
        val opplysning = opplysningerNavnMap[nøkkel] ?: return null
        val sistePeriode = opplysning.perioder.lastOrNull() ?: return null
        val verdi = sistePeriode.verdi
        return when (verdi) {
            is DatoVerdiDTO -> verdi.verdi
            else -> null
        }
    }

    private fun oppslåRåverdi(nøkkel: String): Any? {
        val opplysning = opplysningerNavnMap[nøkkel] ?: return null
        val sistePeriode = opplysning.perioder.lastOrNull() ?: return null
        return when (val verdi = sistePeriode.verdi) {
            is BoolskVerdiDTO -> verdi.verdi
            is DatoVerdiDTO -> verdi.verdi
            is DesimaltallVerdiDTO -> verdi.verdi
            is HeltallVerdiDTO -> verdi.verdi
            is PengeVerdiDTO -> verdi.verdi
            is TekstVerdiDTO -> verdi.verdi
            else -> verdi.toString()
        }
    }

    /**
     * Utfører en makro-transformasjon på en opplysning.
     *
     * Støttede makroer:
     * - månedÅr(offset) — dato +/- N måneder, formatert som "april 2025"
     * - dato(offset) — dato +/- N måneder, formatert som "1. april 2025"
     * - plussDager(n) — dato + N dager, formatert som "1. april 2025"
     * - storFørsteBokstav() — kapitaliserer første bokstav
     * - velg(verdi1=tekst1, verdi2=tekst2, ...) — mapper verdi til lesbar tekst
     */
    private fun utførMakro(
        nøkkel: String,
        makro: String,
    ): String? {
        // Prøv velg()-makro først (har annen syntaks)
        val velgMatch = VELG_REGEX.matchEntire(makro)
        if (velgMatch != null) {
            val råverdi = oppslåRåverdi(nøkkel)?.toString() ?: return null
            val mappinger = velgMatch.groupValues[1]
            return utførVelg(råverdi, mappinger)
        }

        // Standard makro med numerisk argument
        val standardMatch = MAKRO_REGEX.matchEntire(makro)
        if (standardMatch != null) {
            val funksjonsnavn = standardMatch.groupValues[1].lowercase()
            val argument = standardMatch.groupValues[2].toLongOrNull() ?: 0L
            return utførStandardMakro(nøkkel, funksjonsnavn, argument)
        }

        // Makro uten argument (f.eks. storFørsteBokstav)
        val utenArgMatch = MAKRO_UTEN_ARG_REGEX.matchEntire(makro)
        if (utenArgMatch != null) {
            val funksjonsnavn = utenArgMatch.groupValues[1].lowercase()
            return utførMakroUtenArg(nøkkel, funksjonsnavn)
        }

        return null
    }

    private fun utførStandardMakro(
        nøkkel: String,
        funksjonsnavn: String,
        argument: Long,
    ): String? {
        val dato = oppslåDato(nøkkel) ?: return null
        return when (funksjonsnavn) {
            "månedår", "måned_år", "maanedaar" -> {
                val justert = dato.plusMonths(argument)
                justert.format(NORSK_MÅNED_ÅR)
            }
            "dato" -> {
                val justert = dato.plusMonths(argument)
                formaterDato(justert)
            }
            "plussdager", "dager" -> {
                val justert = dato.plusDays(argument)
                formaterDato(justert)
            }
            "plussuker", "uker" -> {
                val justert = dato.plusWeeks(argument)
                formaterDato(justert)
            }
            else -> null
        }
    }

    private fun utførMakroUtenArg(
        nøkkel: String,
        funksjonsnavn: String,
    ): String? {
        val verdi = oppslåVerdi(nøkkel) ?: return null
        return when (funksjonsnavn) {
            "storførstebokstav", "capitalize" ->
                verdi.replaceFirstChar { it.uppercase(Locale("nb", "NO")) }
            "stor", "uppercase" -> verdi.uppercase(Locale("nb", "NO"))
            "liten", "lowercase" -> verdi.lowercase(Locale("nb", "NO"))
            else -> null
        }
    }

    private fun utførVelg(
        verdi: String,
        mappinger: String,
    ): String? {
        // Parser "verdi1=tekst1, verdi2=tekst2"
        val map =
            mappinger
                .split(",")
                .associate { del ->
                    val (k, v) = del.split("=", limit = 2).map { it.trim() }
                    k to v
                }
        return map[verdi]
    }

    private fun utførStringMakro(
        verdi: String,
        makro: String,
    ): String? {
        val utenArgMatch = MAKRO_UTEN_ARG_REGEX.matchEntire(makro)
        if (utenArgMatch != null) {
            val funksjonsnavn = utenArgMatch.groupValues[1].lowercase()
            return when (funksjonsnavn) {
                "storførstebokstav", "capitalize" ->
                    verdi.replaceFirstChar { it.uppercase(Locale("nb", "NO")) }
                "stor", "uppercase" -> verdi.uppercase(Locale("nb", "NO"))
                "liten", "lowercase" -> verdi.lowercase(Locale("nb", "NO"))
                else -> null
            }
        }
        return null
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
                "fraogmed", "fra", "gyldigfraogmed" -> sistePeriode.gyldigFraOgMed?.let { formaterDato(it) }
                "tilogmed", "til", "gyldigtilogmed" -> sistePeriode.gyldigTilOgMed?.let { formaterDato(it) }
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
            is DatoVerdiDTO -> formaterDato(verdi.verdi)
            is DesimaltallVerdiDTO -> formaterDesimaltall(verdi.verdi)
            is HeltallVerdiDTO -> verdi.verdi.toString()
            is PengeVerdiDTO -> formaterPenger(verdi.verdi)
            is TekstVerdiDTO -> verdi.verdi
            else -> verdi.toString()
        }
    }

    companion object {
        private val PLACEHOLDER_REGEX = Regex("\\{\\{(.+?)}}")
        private val MAKRO_REGEX = Regex("""([\w\p{L}]+)\((-?\d+)\)""")
        private val MAKRO_UTEN_ARG_REGEX = Regex("""([\w\p{L}]+)\(\)""")
        private val VELG_REGEX = Regex("""velg\((.+)\)""")
        private val NORSK_DATO = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("nb", "NO"))
        private val NORSK_MÅNED_ÅR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("nb", "NO"))
        private val NORSK_TALL = java.text.NumberFormat.getIntegerInstance(Locale("nb", "NO"))

        private fun formaterDato(dato: LocalDate): String = dato.format(NORSK_DATO)

        private fun formaterPenger(verdi: java.math.BigDecimal): String =
            NORSK_TALL
                .format(verdi.setScale(0, java.math.RoundingMode.HALF_UP).toBigInteger())
                .replace('\u00A0', ' ')

        private fun formaterDesimaltall(verdi: Double): String {
            val rounded =
                java.math.BigDecimal
                    .valueOf(verdi)
                    .setScale(2, java.math.RoundingMode.HALF_UP)
            return rounded.stripTrailingZeros().toPlainString()
        }
    }
}
