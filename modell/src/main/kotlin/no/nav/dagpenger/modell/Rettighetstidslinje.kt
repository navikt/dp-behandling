package no.nav.dagpenger.modell

import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkIdent
import no.nav.dagpenger.opplysning.Rettighetsperiode
import java.time.LocalDate
import java.util.UUID

data class Rettighetstatus(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val harRett: Boolean,
    val behandlingId: UUID,
    val behandlingskjedeId: UUID,
    // Id-en til opplysningen perioden stammer fra, se Rettighetsperiode.opplysningId.
    val opplysningId: UUID,
)

/**
 * Holder, per [RegelverkIdent], de periodene (fraOgMed/tilOgMed) som til sammen utgjør en persons
 * rettighetshistorikk. Bygges opp inkrementelt ved at hver ferdigstilte behandling rapporterer sin
 * FULLSTENDIGE periodeliste (se [oppdater]) - tidslinjen erstatter da kun den delen av historikken som
 * tilhører behandlingens kjede, slik at flere kjeder innenfor samme regelverk (f.eks. en avsluttet sak
 * etterfulgt av en ny) kan leve side om side uten å overskrive hverandre.
 *
 * Mangler det en periode for en gitt dato, har personen ikke rett - det finnes ingen syntetiske
 * "opphører fra"-perioder. En periode som videreføres uendret fra en tidligere behandling i samme kjede
 * ("arvet") beholder opphavet (behandlingId/behandlingskjedeId) til behandlingen som faktisk skapte den.
 */
class Rettighetstidslinje private constructor(
    private val perRegelverk: MutableMap<RegelverkIdent, MutableList<Rettighetstatus>>,
) {
    constructor() : this(mutableMapOf())

    fun harRett(
        regelverk: RegelverkIdent,
        dato: LocalDate,
    ): Boolean = perRegelverk[regelverk]?.any { it.harRett && dato in it.fraOgMed..it.tilOgMed } ?: false

    fun harRett(
        regelverk: Regelverk,
        dato: LocalDate,
    ): Boolean = harRett(regelverk.ident, dato)

    fun perioder(regelverk: RegelverkIdent): List<Rettighetstatus> = perRegelverk[regelverk]?.sortedBy { it.fraOgMed } ?: emptyList()

    fun perioder(regelverk: Regelverk): List<Rettighetstatus> = perioder(regelverk.ident)

    // Hvilke regelverk tidslinjen har perioder for, brukes ved persistering av hele tidslinjen på tvers av regelverk.
    fun regelverk(): Set<RegelverkIdent> = perRegelverk.keys.toSet()

    // Skoper tidslinjen til ett regelverk, slik at hendelser kan spørre om rett uten selv å vite hvilket regelverk de gjelder for.
    fun forRegelverk(regelverk: RegelverkIdent): Rettighetsperioder = Rettighetsperioder(perRegelverk[regelverk] ?: emptyList())

    fun forRegelverk(regelverk: Regelverk): Rettighetsperioder = forRegelverk(regelverk.ident)

    internal fun oppdater(
        regelverk: RegelverkIdent,
        behandlingId: UUID,
        behandlingskjedeId: UUID,
        perioder: List<Rettighetsperiode>,
    ) {
        val eksisterende = perRegelverk.getOrPut(regelverk) { mutableListOf() }

        // Opphavet til en periode som videreføres uendret (samme opplysningId) hentes fra det som
        // allerede står i tidslinjen for denne kjeden, FØR vi fjerner kjedens gamle perioder.
        val forrigeOpphav = eksisterende.filter { it.behandlingskjedeId == behandlingskjedeId }.associateBy { it.opplysningId }

        eksisterende.removeIf { it.behandlingskjedeId == behandlingskjedeId }

        perioder.forEach { periode ->
            val opphav = forrigeOpphav[periode.opplysningId]
            eksisterende.add(
                Rettighetstatus(
                    fraOgMed = periode.fraOgMed,
                    tilOgMed = periode.tilOgMed,
                    harRett = periode.harRett,
                    behandlingId = opphav?.behandlingId ?: behandlingId,
                    behandlingskjedeId = opphav?.behandlingskjedeId ?: behandlingskjedeId,
                    opplysningId = periode.opplysningId,
                ),
            )
        }
    }

    companion object {
        fun fraPerioder(perRegelverk: Map<RegelverkIdent, List<Rettighetstatus>>): Rettighetstidslinje =
            Rettighetstidslinje(perRegelverk.mapValuesTo(mutableMapOf()) { (_, perioder) -> perioder.toMutableList() })
    }
}

/**
 * En read-only visning av [Rettighetstidslinje] skopet til ett regelverk. Brukes av [no.nav.dagpenger.modell.hendelser.StartHendelse]
 * slik at hendelser kan spørre om rett uten å måtte vite om eller videreformidle hvilket regelverk de selv gjelder for.
 */
class Rettighetsperioder internal constructor(
    private val perioder: List<Rettighetstatus>,
) {
    fun harRett(dato: LocalDate): Boolean = perioder.any { it.harRett && dato in it.fraOgMed..it.tilOgMed }

    fun harAldriHattRett(): Boolean = perioder.none { it.harRett }
}
